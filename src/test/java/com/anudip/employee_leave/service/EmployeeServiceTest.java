package com.anudip.employee_leave.service;

import com.anudip.employee_leave.dto.EmployeeDTO;
import com.anudip.employee_leave.entity.Department;
import com.anudip.employee_leave.entity.Employee;
import com.anudip.employee_leave.entity.EmploymentStatus;
import com.anudip.employee_leave.entity.EmploymentType;
import com.anudip.employee_leave.exception.DuplicateResourceException;
import com.anudip.employee_leave.repository.DepartmentRepository;
import com.anudip.employee_leave.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
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
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private EmployeeService employeeService;

    private Department department;
    private Employee employee;

    @BeforeEach
    void setUp() {
        department = new Department("Engineering", "Software development division");
        department.setId(1L);

        employee = new Employee();
        employee.setId(1L);
        employee.setEmployeeId("EMP-001");
        employee.setFullName("Jane Doe");
        employee.setEmail("jane.doe@workforce.com");
        employee.setDepartment(department);
        employee.setEmploymentType(EmploymentType.FULL_TIME);
        employee.setJoiningDate(LocalDate.of(2024, 1, 15));
        employee.setEmploymentStatus(EmploymentStatus.ACTIVE);
    }

    @Test
    void testCreateEmployeeSuccess() {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setEmployeeId("EMP-001");
        dto.setFullName("Jane Doe");
        dto.setEmail("jane.doe@workforce.com");
        dto.setDepartmentId(1L);
        dto.setEmploymentType(EmploymentType.FULL_TIME);
        dto.setJoiningDate(LocalDate.of(2024, 1, 15));
        dto.setEmploymentStatus(EmploymentStatus.ACTIVE);

        when(employeeRepository.existsByEmployeeIdIgnoreCase("EMP-001")).thenReturn(false);
        when(employeeRepository.existsByEmailIgnoreCase("jane.doe@workforce.com")).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

        Employee result = employeeService.createEmployee(dto);

        assertNotNull(result);
        assertEquals("EMP-001", result.getEmployeeId());
        assertEquals("Jane Doe", result.getFullName());
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    void testCreateEmployeeDuplicateEmployeeIdThrowsException() {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setEmployeeId("EMP-001");
        dto.setEmail("new.email@workforce.com");

        when(employeeRepository.existsByEmployeeIdIgnoreCase("EMP-001")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> employeeService.createEmployee(dto));
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void testCreateEmployeeDuplicateEmailThrowsException() {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setEmployeeId("EMP-002");
        dto.setEmail("jane.doe@workforce.com");

        when(employeeRepository.existsByEmployeeIdIgnoreCase("EMP-002")).thenReturn(false);
        when(employeeRepository.existsByEmailIgnoreCase("jane.doe@workforce.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> employeeService.createEmployee(dto));
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void testUpdateEmployeeCannotBeOwnManagerThrowsException() {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setId(1L);
        dto.setEmployeeId("EMP-001");
        dto.setFullName("Jane Doe");
        dto.setEmail("jane.doe@workforce.com");
        dto.setDepartmentId(1L);
        dto.setReportingManagerId(1L); // Self as manager

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByEmployeeIdIgnoreCaseAndIdNot("EMP-001", 1L)).thenReturn(false);
        when(employeeRepository.existsByEmailIgnoreCaseAndIdNot("jane.doe@workforce.com", 1L)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> employeeService.updateEmployee(1L, dto));

        assertEquals("An employee cannot be their own reporting manager.", ex.getMessage());
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void testToggleEmployeeStatusActiveToInactive() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        employeeService.toggleEmployeeStatus(1L);

        assertEquals(EmploymentStatus.INACTIVE, employee.getEmploymentStatus());
        verify(employeeRepository).save(employee);
    }

    @Test
    void testToggleEmployeeStatusInactiveToActive() {
        employee.setEmploymentStatus(EmploymentStatus.INACTIVE);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        employeeService.toggleEmployeeStatus(1L);

        assertEquals(EmploymentStatus.ACTIVE, employee.getEmploymentStatus());
        verify(employeeRepository).save(employee);
    }

    @Test
    void testGetFilteredEmployees() {
        when(employeeRepository.findFiltered(1L, EmploymentStatus.ACTIVE)).thenReturn(List.of(employee));

        List<Employee> results = employeeService.getFilteredEmployees(1L, EmploymentStatus.ACTIVE);

        assertEquals(1, results.size());
        assertEquals("EMP-001", results.get(0).getEmployeeId());
    }
}

