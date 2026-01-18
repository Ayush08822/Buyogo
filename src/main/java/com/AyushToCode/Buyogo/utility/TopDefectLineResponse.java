package com.AyushToCode.Buyogo.utility;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopDefectLineResponse {

    /**
     * The ID of the production line (Derived from machineId) [cite: 98]
     */
    private String lineId;

    /**
     * Total number of defects in the window, ignoring -1 values [cite: 99, 109]
     */
    private long totalDefects;

    /**
     * Total number of valid events in the window (post-deduplication) [cite: 100]
     */
    private long eventCount;

    /**
     * Defect per 100 Events, rounded to 2 decimals
     * Calculation: (totalDefects / eventCount) * 100
     */
    private double defectsPercent;
}