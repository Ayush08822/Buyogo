package com.AyushToCode.Buyogo.repo;

import com.AyushToCode.Buyogo.entity.MachineEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface MachineRepository extends JpaRepository<MachineEvent, String> {
    List<MachineEvent> findAllByEventIdIn(List<String> eventIds);

    @Query("SELECT COUNT(e), SUM(CASE WHEN e.defectCount != -1 THEN e.defectCount ELSE 0 END) " +
            "FROM MachineEvent e " +
            "WHERE e.machineId = :mId AND e.eventTime >= :start AND e.eventTime < :end")
    List<Object[]> getStatsData(@Param("mId") String machineId,
                          @Param("start") Instant start,
                          @Param("end") Instant end);

    @Query("SELECT e.lineId, " +
            "SUM(CASE WHEN e.defectCount != -1 THEN e.defectCount ELSE 0 END), " +
            "COUNT(e) " +
            "FROM MachineEvent e " +
            "WHERE e.factoryId = :fId AND e.eventTime >= :from AND e.eventTime < :to " +
            "GROUP BY e.lineId " +
            "ORDER BY SUM(CASE WHEN e.defectCount != -1 THEN e.defectCount ELSE 0 END) DESC")
    List<Object[]> findTopDefectLines(@Param("fId") String factoryId,
                                      @Param("from") Instant from,
                                      @Param("to") Instant to);
}
