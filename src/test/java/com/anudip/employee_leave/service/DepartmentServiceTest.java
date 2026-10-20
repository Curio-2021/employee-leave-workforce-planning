package com.anudip.employee_leave.service;

import com.anudip.employee_leave.dto.DepartmentDTO;
import com.anudip.employee_leave.entity.Department;
import com.anudip.employee_leave.exception.DuplicateResourceException;
import com.anudip.employee_leave.exception.ResourceNotFoundException;
import com.anudip.employee_leave.repository.DepartmentRepository;
import com.anudip.employee_leave.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private DepartmentService departmentService;

    private Department department;

    @BeforeEach
    void setUp() {
        department = new Department("Engineering", "Software development division");
        department.setId(1L);
    }

    @Test
    void testCreateDepartmentSuccess() {
        DepartmentDTO dto = new DepartmentDTO("Engineering", "Software development division");
        when(departmentRepository.existsByNameIgnoreCase("Engineering")).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenReturn(department);

        Department result = departmentService.createDepartment(dto);

        assertNotNull(result);
        assertEquals("Engineering", result.getName());
        verify(departmentRepository).save(any(Department.class));
    }

    @Test
    void testCreateDepartmentDuplicateNameThrowsException() {
        DepartmentDTO dto = new DepartmentDTO("Engineering", "Duplicate dept");
        when(departmentRepository.existsByNameIgnoreCase("Engineering")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> departmentService.createDepartment(dto));
        verify(departmentRepository, never()).save(any(Department.class));
    }

    @Test
    void testGetAllDepartments() {
        when(departmentRepository.findAllByOrderByNameAsc()).thenReturn(List.of(department));

        List<Department> result = departmentService.getAllDepartments();

        assertEquals(1, result.size());
        assertEquals("Engineering", result.get(0).getName());
    }

    @Test
    void testGetDepartmentByIdNotFoundThrowsException() {
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> departmentService.getDepartmentById(99L));
    }
}

