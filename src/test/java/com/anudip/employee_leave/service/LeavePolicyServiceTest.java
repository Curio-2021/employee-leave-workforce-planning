package com.anudip.employee_leave.service;

import com.anudip.employee_leave.dto.LeavePolicyDTO;
import com.anudip.employee_leave.entity.LeavePolicy;
import com.anudip.employee_leave.entity.LeaveType;
import com.anudip.employee_leave.exception.DuplicateResourceException;
import com.anudip.employee_leave.repository.DepartmentRepository;
import com.anudip.employee_leave.repository.LeavePolicyRepository;
import com.anudip.employee_leave.repository.LeaveTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeavePolicyServiceTest {

    @Mock
    private LeavePolicyRepository leavePolicyRepository;

    @Mock
    private LeaveTypeRepository leaveTypeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private LeavePolicyService leavePolicyService;

    private LeaveType leaveType;
    private LeavePolicy policy;

    @BeforeEach
    void setUp() {
        leaveType = new LeaveType("Earned Leave", "EL", "Vacation quota", true, true, 14, false);
        leaveType.setId(1L);

        policy = new LeavePolicy("Standard EL 2026", leaveType, null, null, 15, true, 5, 90, 2026, true);
        policy.setId(1L);
    }

    @Test
    void testCreatePolicySuccess() {
        LeavePolicyDTO dto = new LeavePolicyDTO();
        dto.setPolicyName("Standard EL 2026");
        dto.setLeaveTypeId(1L);
        dto.setAnnualAllocation(15);
        dto.setAllowCarryForward(true);
        dto.setMaxCarryForwardDays(5);
        dto.setMinServiceDays(90);
        dto.setEffectiveYear(2026);
        dto.setActive(true);

        when(leavePolicyRepository.existsByPolicyNameIgnoreCaseAndEffectiveYear("Standard EL 2026", 2026)).thenReturn(false);
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(leaveType));
        when(leavePolicyRepository.save(any(LeavePolicy.class))).thenReturn(policy);

        LeavePolicy result = leavePolicyService.createPolicy(dto);

        assertNotNull(result);
        assertEquals("Standard EL 2026", result.getPolicyName());
        assertEquals(15, result.getAnnualAllocation());
        verify(leavePolicyRepository).save(any(LeavePolicy.class));
    }

    @Test
    void testCreatePolicyDuplicateNameInYearThrowsException() {
        LeavePolicyDTO dto = new LeavePolicyDTO();
        dto.setPolicyName("Standard EL 2026");
        dto.setEffectiveYear(2026);

        when(leavePolicyRepository.existsByPolicyNameIgnoreCaseAndEffectiveYear("Standard EL 2026", 2026)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> leavePolicyService.createPolicy(dto));
        verify(leavePolicyRepository, never()).save(any(LeavePolicy.class));
    }

    @Test
    void testTogglePolicyStatus() {
        when(leavePolicyRepository.findById(1L)).thenReturn(Optional.of(policy));

        leavePolicyService.togglePolicyStatus(1L);

        assertFalse(policy.isActive());
        verify(leavePolicyRepository).save(policy);
    }
}

