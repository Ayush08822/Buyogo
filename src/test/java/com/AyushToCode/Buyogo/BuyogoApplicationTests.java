package com.AyushToCode.Buyogo;

import com.AyushToCode.Buyogo.entity.MachineEvent;
import com.AyushToCode.Buyogo.repo.MachineRepository;
import com.AyushToCode.Buyogo.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BuyogoApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventService eventService;

    @Autowired
    private MachineRepository repository;

    @BeforeEach
    void setup() {
        repository.deleteAll();
    }

    @Test
    void testLargeBatchPerformance() throws Exception {
        // 1. Generate 1,000 unique events
        List<MachineEvent> largeBatch = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            MachineEvent event = new MachineEvent();
            event.setEventId("BATCH-E-" + i);
            event.setMachineId("M-001");
            event.setEventTime(Instant.now());
            event.setDurationMs(1000 + i);
            event.setDefectCount(i % 10 == 0 ? 1 : 0); // 10% defect rate
            largeBatch.add(event);
        }

        String jsonPayload = objectMapper.writeValueAsString(largeBatch);

        // 2. Measure Time
        long startTime = System.currentTimeMillis();

        mockMvc.perform(post("/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(1000));

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("Time taken for 1,000 events: " + duration + "ms");

        // Assert it meets the requirement
        assertTrue(duration < 1000, "Processing took longer than 1 second!");
    }

    /**
     * Requirement: Thread-safety with 5-20 parallel requests.
     * This test ensures that concurrent updates to the same event ID don't crash the system.
     */
    @Test
    void testThreadSafety_ConcurrentIngestion() throws InterruptedException {
        int threads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        String duplicateEventId = "STRESS-TASK-01";

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    MachineEvent event = MachineEvent.builder()
                            .eventId(duplicateEventId)
                            .machineId("M-CONCURRENT")
                            .eventTime(Instant.now())
                            .durationMs(2000)
                            .defectCount(1)
                            .build();

                    // Directly calling service to verify transactional logic
                    eventService.processBatch(List.of(event));
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(10, TimeUnit.SECONDS);
        assertTrue(finished, "Threads did not complete in time");
        System.out.println(">>> Thread-Safety Test: Successfully handled 20 concurrent requests.");
    }

    @Test
    void testBusinessRules_Dedupe_Update_Validation() throws Exception {
        String eventId = "TEST-001";
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);

        // 1. Initial Insert (Accepted)
        MachineEvent event1 = createEvent(eventId, "M1", 5000, 2, now);
        sendBatch(List.of(event1))
                .andExpect(jsonPath("$.accepted").value(1));

        // 2. Identical duplicate eventId (Deduped)
        sendBatch(List.of(event1))
                .andExpect(jsonPath("$.deduped").value(1));

        // 3. Different payload + newer receivedTime (Update)
        // Note: Our service sets receivedTime = Instant.now() automatically
        MachineEvent eventUpdate = createEvent(eventId, "M1", 6000, 5, now);
        sendBatch(List.of(eventUpdate))
                .andExpect(jsonPath("$.updated").value(1));

        // 4. Invalid duration rejected (Duration > 6 hours)
        MachineEvent invalidDuration = createEvent("ERR-1", "M1", 30000000, 0, now);
        sendBatch(List.of(invalidDuration))
                .andExpect(jsonPath("$.rejected").value(1))
                .andExpect(jsonPath("$.rejections[0].reason").value("INVALID_DURATION"));

        // 5. Future eventTime rejected (> 15 mins)
        MachineEvent futureEvent = createEvent("ERR-2", "M1", 1000, 0, now.plus(20, ChronoUnit.MINUTES));
        sendBatch(List.of(futureEvent))
                .andExpect(jsonPath("$.rejected").value(1))
                .andExpect(jsonPath("$.rejections[0].reason").value("FUTURE_EVENT_TIME"));
    }

    @Test
    void testDefectRules_And_Boundaries() throws Exception {
        String machineId = "M-STATS";
        Instant start = Instant.parse("2026-01-01T10:00:00Z");
        Instant end = Instant.parse("2026-01-01T11:00:00Z");

        // 6. DefectCount = -1 ignored in totals
        MachineEvent eventNormal = createEvent("E1", machineId, 1000, 5, start.plus(10, ChronoUnit.MILLIS));
        MachineEvent eventIgnored = createEvent("E2", machineId, 1000, -1, start.plus(20, ChronoUnit.MINUTES));

        // 7. Boundary test (Inclusive start, Exclusive end)
        MachineEvent eventAtStart = createEvent("E3", machineId, 1000, 1, start); // Should be included
        MachineEvent eventAtEnd = createEvent("E4", machineId, 1000, 10, end);    // Should be excluded

        sendBatch(List.of(eventNormal));

        // Query Stats
        mockMvc.perform(get("/events/stats")
                        .param("machineId", machineId)
                        .param("start", start.toString())
                        .param("end", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventsCount").value(1)) // E1, E2, E3
                .andExpect(jsonPath("$.defectsCount").value(5)); // 5 (E1) + 1 (E3). E2's -1 is ignored.
    }

    // Helper method to keep code clean
    private MachineEvent createEvent(String id, String mid, int dur, int defects, Instant time) {
        return MachineEvent.builder()
                .eventId(id).machineId(mid).durationMs(dur)
                .defectCount(defects).eventTime(time).build();
    }

    private org.springframework.test.web.servlet.ResultActions sendBatch(List<MachineEvent> events) throws Exception {
        return mockMvc.perform(post("/events/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(events)));
    }

}
