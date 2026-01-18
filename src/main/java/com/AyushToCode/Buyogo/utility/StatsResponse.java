package com.AyushToCode.Buyogo.utility;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatsResponse {
    private String machineId;     // The ID of the machine queried

    private String start;         // Start of time window (ISO-8601)

    private String end;           // End of time window (ISO-8601)

    private long eventsCount;     // Total valid events in window

    private long defectsCount;    // Sum of defects, excluding -1 values

    private double avgDefectRate; // Defects per hour

    private String status;        // "Healthy" or "Warning"
}
