package com.AyushToCode.Buyogo.controller;

import com.AyushToCode.Buyogo.entity.MachineEvent;
import com.AyushToCode.Buyogo.service.EventService;
import com.AyushToCode.Buyogo.utility.BatchResponse;
import com.AyushToCode.Buyogo.utility.StatsResponse;
import com.AyushToCode.Buyogo.utility.TopDefectLineResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class MachineController {

    private final EventService eventService;

    @PostMapping("/batch")
    public ResponseEntity<BatchResponse> ingestBatch(@RequestBody List<MachineEvent> events) {
        BatchResponse response = eventService.processBatch(events);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<StatsResponse> getMachineStats(
            @RequestParam String machineId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {

        // Start is inclusive, end is exclusive
        return ResponseEntity.ok(eventService.getMachineStats(machineId, start, end));
    }

    @GetMapping("/stats/top-defect-lines")
    public ResponseEntity<List<TopDefectLineResponse>> getTopDefectLines(
            @RequestParam String factoryId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(eventService.getTopDefectLines(factoryId, from, to, limit));
    }
}
