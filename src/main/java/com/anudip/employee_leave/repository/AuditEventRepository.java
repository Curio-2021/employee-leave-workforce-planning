package com.anudip.employee_leave.repository;

import com.anudip.employee_leave.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    List<AuditEvent> findAllByOrderByEventTimestampDesc();

    @Query("SELECT a FROM AuditEvent a WHERE " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:entityType IS NULL OR a.entityType = :entityType) AND " +
           "(:departmentId IS NULL OR a.departmentId = :departmentId) AND " +
           "(CAST(:fromDate AS timestamp) IS NULL OR a.eventTimestamp >= :fromDate) AND " +
           "(CAST(:toDate AS timestamp) IS NULL OR a.eventTimestamp <= :toDate) " +
           "ORDER BY a.eventTimestamp DESC")
    List<AuditEvent> findFiltered(@Param("action") String action,
                                  @Param("entityType") String entityType,
                                  @Param("departmentId") Long departmentId,
                                  @Param("fromDate") LocalDateTime fromDate,
                                  @Param("toDate") LocalDateTime toDate);

    List<AuditEvent> findTop50ByOrderByEventTimestampDesc();
}

