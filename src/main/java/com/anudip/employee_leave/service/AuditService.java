package com.anudip.employee_leave.service;

import com.anudip.employee_leave.entity.AuditEvent;
import com.anudip.employee_leave.repository.AuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final AuditActorResolver auditActorResolver;

    public AuditService(AuditEventRepository auditEventRepository, AuditActorResolver auditActorResolver) {
        this.auditEventRepository = auditEventRepository;
        this.auditActorResolver = auditActorResolver;
    }

    /**
     * Records an audit event within the caller's active database transaction.
     * Guaranteed to rollback if the surrounding business operation rolls back.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public AuditEvent recordEvent(String actor, String action, String entityType,
                                  String entityId, Long departmentId, String details) {
        String resolvedActor = auditActorResolver.resolveUserActor(actor);
        AuditEvent event = new AuditEvent(resolvedActor, action, entityType, entityId, departmentId, details);
        return auditEventRepository.save(event);
    }

    /**
     * Records a system-generated audit event.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public AuditEvent recordSystemEvent(String componentName, String action, String entityType,
                                        String entityId, Long departmentId, String details) {
        String resolvedActor = auditActorResolver.resolveSystemActor(componentName);
        AuditEvent event = new AuditEvent(resolvedActor, action, entityType, entityId, departmentId, details);
        return auditEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> getRecentEvents() {
        return auditEventRepository.findTop50ByOrderByEventTimestampDesc();
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> getFilteredEvents(String action, String entityType, Long departmentId,
                                             LocalDate fromDate, LocalDate toDate) {
        LocalDateTime fromTime = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime toTime = toDate != null ? toDate.atTime(23, 59, 59) : null;
        String act = (action != null && !action.isBlank()) ? action.trim() : null;
        String ent = (entityType != null && !entityType.isBlank()) ? entityType.trim() : null;
        return auditEventRepository.findFiltered(act, ent, departmentId, fromTime, toTime);
    }
}

