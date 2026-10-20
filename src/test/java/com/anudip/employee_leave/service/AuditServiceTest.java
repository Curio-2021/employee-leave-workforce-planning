package com.anudip.employee_leave.service;

import com.anudip.employee_leave.entity.AuditEvent;
import com.anudip.employee_leave.repository.AuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    private AuditActorResolver auditActorResolver;
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditActorResolver = new AuditActorResolver();
        auditService = new AuditService(auditEventRepository, auditActorResolver);
    }

    @Test
    void testRecordEvent_TagsUnauthenticatedActorCorrectly() {
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditEvent event = auditService.recordEvent("Alice Smith (EMP101)", "LEAVE_SUBMITTED",
                "LeaveRequest", "LR-001", 1L, "Submitted 2 days leave");

        assertThat(event.getActor()).startsWith("DEV_UNAUTHENTICATED: ");
        assertThat(event.getActor()).contains("Alice Smith (EMP101)");
        assertThat(event.getAction()).isEqualTo("LEAVE_SUBMITTED");
        assertThat(event.getEntityType()).isEqualTo("LeaveRequest");
        assertThat(event.getEntityId()).isEqualTo("LR-001");
        assertThat(event.getDepartmentId()).isEqualTo(1L);
        assertThat(event.getDetails()).isEqualTo("Submitted 2 days leave");
        assertThat(event.getEventTimestamp()).isNotNull();

        verify(auditEventRepository).save(any(AuditEvent.class));
    }

    @Test
    void testRecordSystemEvent_TagsSystemActorCorrectly() {
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditEvent event = auditService.recordSystemEvent("WorkforceEngine", "NIGHTLY_CALCULATION",
                "WorkforceRoster", "ALL", null, "Nightly availability snapshot computed");

        assertThat(event.getActor()).isEqualTo("SYSTEM_WORKFLOW: WorkforceEngine");
        assertThat(event.getAction()).isEqualTo("NIGHTLY_CALCULATION");

        verify(auditEventRepository).save(any(AuditEvent.class));
    }

    @Test
    void testGetFilteredEvents_ConvertsDatesToTimestamps() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 31);

        auditService.getFilteredEvents("LEAVE_APPROVED", "LeaveRequest", 2L, from, to);

        ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> entityCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> deptCaptor = ArgumentCaptor.forClass(Long.class);

        verify(auditEventRepository).findFiltered(
                actionCaptor.capture(),
                entityCaptor.capture(),
                deptCaptor.capture(),
                any(),
                any()
        );

        assertThat(actionCaptor.getValue()).isEqualTo("LEAVE_APPROVED");
        assertThat(entityCaptor.getValue()).isEqualTo("LeaveRequest");
        assertThat(deptCaptor.getValue()).isEqualTo(2L);
    }

    @Test
    void testGetRecentEvents_DelegatesToRepository() {
        AuditEvent event = new AuditEvent("DEV_UNAUTHENTICATED: HR", "TEST", "Test", "1", 1L, "Details");
        when(auditEventRepository.findTop50ByOrderByEventTimestampDesc()).thenReturn(List.of(event));

        List<AuditEvent> recent = auditService.getRecentEvents();
        assertThat(recent).hasSize(1);
        assertThat(recent.get(0).getAction()).isEqualTo("TEST");
    }
}

