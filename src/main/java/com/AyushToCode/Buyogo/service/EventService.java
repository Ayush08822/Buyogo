package com.AyushToCode.Buyogo.service;

import com.AyushToCode.Buyogo.entity.MachineEvent;
import com.AyushToCode.Buyogo.repo.MachineRepository;
import com.AyushToCode.Buyogo.utility.BatchResponse;
import com.AyushToCode.Buyogo.utility.StatsResponse;
import com.AyushToCode.Buyogo.utility.TopDefectLineResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private final MachineRepository repository;

    @Transactional
    public BatchResponse processBatch(List<MachineEvent> events) {
        BatchResponse summary = new BatchResponse();

        // 1. Collect all IDs to check the DB in one single trip
        List<String> incomingIds = events.stream()
                .map(MachineEvent::getEventId)
                .toList();

        // 2. Fetch all existing records at once for speed
        List<MachineEvent> existingEvents = repository.findAllById(incomingIds);
        Map<String, MachineEvent> existingMap = existingEvents.stream()
                .collect(Collectors.toMap(MachineEvent::getEventId, e -> e));

        List<MachineEvent> toSave = new ArrayList<>();

        for (MachineEvent event : events) {
            // Validation: Duration
            if (event.getDurationMs() < 0 || event.getDurationMs() > 21600000) {
                summary.addRejection(event.getEventId(), "INVALID_DURATION");
                continue;
            }

            // Enrich the data with factory and line mapping
            enrichEventData(event);

            // Validation: Future Time (15 min limit)
            if (event.getEventTime().isAfter(Instant.now().plus(15, ChronoUnit.MINUTES))) {
                summary.addRejection(event.getEventId(), "FUTURE_EVENT_TIME");
                continue;
            }

            // Set receivedTime (Ignore input value from JSON)
            event.setReceivedTime(Instant.now());

            MachineEvent existing = existingMap.get(event.getEventId());

            if (existing == null) {
                // New record
                toSave.add(event);
                summary.incrementAccepted();
            } else if (isIdentical(existing, event)) {
                // Identical ID + Payload -> Ignore
                System.out.println("HIII");
                summary.incrementDeduped();
            } else {
                // Same ID + Different Payload -> Update
                toSave.add(event);
                summary.incrementUpdated();
            }
        }

        // Batch save the list in one go
        repository.saveAll(toSave);
        return summary;
    }

    private boolean isIdentical(MachineEvent existing, MachineEvent incoming) {
        return existing.getMachineId().equals(incoming.getMachineId()) &&
                existing.getDurationMs() == incoming.getDurationMs() &&
                existing.getDefectCount() == incoming.getDefectCount() &&
                existing.getEventTime().equals(incoming.getEventTime());
    }

    private void enrichEventData(MachineEvent event) {
        // Simple logic: Machines M-001 to M-010 belong to Line 1
        // You can customize this mapping logic
        String mid = event.getMachineId();
        event.setFactoryId("F01"); // Assuming a single factory for now

        if (mid.compareTo("M-010") <= 0) {
            event.setLineId("LINE-01");
        } else {
            event.setLineId("LINE-02");
        }
    }

    public List<TopDefectLineResponse> getTopDefectLines(String factoryId, Instant from, Instant to, int limit) {
        List<Object[]> results = repository.findTopDefectLines(factoryId, from, to);

        return results.stream()
                .limit(limit) // Respect the limit parameter [cite: 95]
                .map(row -> {
                    String lineId = (String) row[0];
                    long totalDefects = (row[1] != null) ? (long) row[1] : 0;
                    long eventCount = (long) row[2];

                    // Calculation: Defects per 100 Events
                    double defectsPercent = (eventCount > 0)
                            ? (totalDefects * 100.0 / eventCount)
                            : 0.0;

                    return TopDefectLineResponse.builder()
                            .lineId(lineId)
                            .totalDefects(totalDefects)
                            .eventCount(eventCount)
                            .defectsPercent(Math.round(defectsPercent * 100.0) / 100.0) // Round to 2 decimals
                            .build();
                })
                .collect(Collectors.toList());
    }

    public StatsResponse getMachineStats(String machineId, Instant start, Instant end) {
        List<Object[]> results = repository.getStatsData(machineId, start, end);
        long eventsCount = 0;
        long defectsCount = 0;

        if (results != null && !results.isEmpty()) {
            Object[] row = results.getFirst();
            // Safely convert to Long using Number
            eventsCount = (row[0] != null) ? ((Number) row[0]).longValue() : 0;
            defectsCount = (row[1] != null) ? ((Number) row[1]).longValue() : 0;
        }

        // Calculate window hours
        double windowHours = Duration.between(start, end).toSeconds() / 3600.0;

        // Calculate rate (handle division by zero if start == end)
        double avgDefectRate = (windowHours > 0) ? (defectsCount / windowHours) : 0.0;

        // Determine status
        String status = (avgDefectRate < 2.0) ? "Healthy" : "Warning";

        return StatsResponse.builder()
                .machineId(machineId)
                .eventsCount(eventsCount)
                .defectsCount(defectsCount)
                .avgDefectRate(Math.round(avgDefectRate * 100.0) / 100.0) // Round to 2 decimals
                .status(status)
                .build();
    }
}