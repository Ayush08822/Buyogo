package com.AyushToCode.Buyogo.utility;

import java.util.ArrayList;
import java.util.List;

public class BatchResponse {
    private int accepted = 0; // New events saved [cite: 59]
    private int deduped = 0;  // Identical IDs and payloads ignored [cite: 60]
    private int updated = 0;  // Same ID but different payload [cite: 61]
    private int rejected = 0; // Failed validation (duration/time) [cite: 62]
    private List<Rejection> rejections = new ArrayList<>(); // List of errors [cite: 63]

    // Helper method to add a rejection
    public void addRejection(String eventId, String reason) {
        this.rejections.add(new Rejection(eventId, reason));
        this.rejected++;
    }

    // Standard Getters and increment helpers
    public void incrementAccepted() { this.accepted++; }
    public void incrementDeduped() { this.deduped++; }
    public void incrementUpdated() { this.updated++; }

    public int getAccepted() { return accepted; }
    public int getDeduped() { return deduped; }
    public int getUpdated() { return updated; }
    public int getRejected() { return rejected; }
    public List<Rejection> getRejections() { return rejections; }

    // Nested class for specific rejection details [cite: 64]
    public static class Rejection {
        private String eventId;
        private String reason;

        public Rejection(String eventId, String reason) {
            this.eventId = eventId;
            this.reason = reason;
        }

        public String getEventId() { return eventId; }
        public String getReason() { return reason; }
    }
}
