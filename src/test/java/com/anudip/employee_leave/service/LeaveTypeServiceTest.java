package com.anudip.employee_leave.service;

import com.anudip.employee_leave.dto.LeaveTypeDTO;
import com.anudip.employee_leave.entity.LeaveType;
import com.anudip.employee_leave.exception.DuplicateResourceException;
import com.anudip.employee_leave.exception.ResourceNotFoundException;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveTypeServiceTest {

    @Mock
    private LeaveTypeRepository leaveTypeRepository;

    @InjectMocks
    private LeaveTypeService leaveTypeService;

    private LeaveType leaveType;

    @BeforeEach
    void setUp() {
        leaveType = new LeaveType("Casual Leave", "CL", "For urgent personal matters", true, true, 3, false);
        leaveType.setId(1L);
    }

    @Test
    void testCreateLeaveTypeSuccess() {
        LeaveTypeDTO dto = new LeaveTypeDTO("Casual Leave", "CL", "Desc", true, true, 3, false);

        when(leaveTypeRepository.existsByNameIgnoreCase("Casual Leave")).thenReturn(false);
        when(leaveTypeRepository.existsByCodeIgnoreCase("CL")).thenReturn(false);
        when(leaveTypeRepository.save(any(LeaveType.class))).thenReturn(leaveType);

        LeaveType result = leaveTypeService.createLeaveType(dto);

        assertNotNull(result);
        assertEquals("Casual Leave", result.getName());
        assertEquals("CL", result.getCode());
        verify(leaveTypeRepository).save(any(LeaveType.class));
    }

    @Test
    void testCreateLeaveTypeDuplicateNameThrowsException() {
        LeaveTypeDTO dto = new LeaveTypeDTO("Casual Leave", "NEW", "Desc", true, true, 3, false);
        when(leaveTypeRepository.existsByNameIgnoreCase("Casual Leave")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> leaveTypeService.createLeaveType(dto));
        verify(leaveTypeRepository, never()).save(any(LeaveType.class));
    }

    @Test
    void testCreateLeaveTypeDuplicateCodeThrowsException() {
        LeaveTypeDTO dto = new LeaveTypeDTO("Different Name", "CL", "Desc", true, true, 3, false);
        when(leaveTypeRepository.existsByNameIgnoreCase("Different Name")).thenReturn(false);
        when(leaveTypeRepository.existsByCodeIgnoreCase("CL")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> leaveTypeService.createLeaveType(dto));
        verify(leaveTypeRepository, never()).save(any(LeaveType.class));
    }

    @Test
    void testGetAllLeaveTypes() {
        when(leaveTypeRepository.findAllByOrderByNameAsc()).thenReturn(List.of(leaveType));

        List<LeaveType> results = leaveTypeService.getAllLeaveTypes();

        assertEquals(1, results.size());
        assertEquals("CL", results.get(0).getCode());
    }

    @Test
    void testToggleLeaveTypeStatus() {
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(leaveType));

        leaveTypeService.toggleLeaveTypeStatus(1L);

        assertFalse(leaveType.isActive());
        verify(leaveTypeRepository).save(leaveType);
    }
}

