package com.AyushToCode.Buyogo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MachineEvent {

    @Id
    private String eventId;

    private String machineId;

    private String lineId;

    private String factoryId;

    private Instant eventTime;

    private Instant receivedTime;

    private int durationMs;

    private int defectCount;

}