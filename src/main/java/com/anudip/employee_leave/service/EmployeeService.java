package com.anudip.employee_leave.service;

import com.anudip.employee_leave.dto.EmployeeDTO;
import com.anudip.employee_leave.entity.Department;
import com.anudip.employee_leave.entity.Employee;
import com.anudip.employee_leave.entity.EmploymentStatus;
import com.anudip.employee_leave.entity.Role;
import com.anudip.employee_leave.exception.DuplicateResourceException;
import com.anudip.employee_leave.exception.ResourceNotFoundException;
import com.anudip.employee_leave.repository.DepartmentRepository;
import com.anudip.employee_leave.repository.EmployeeRepository;
import com.anudip.employee_leave.security.AccountProvisioningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final AuditService auditService;
    private final AccountProvisioningService accountProvisioningService;

    public EmployeeService(EmployeeRepository employeeRepository, DepartmentRepository departmentRepository) {
        this(employeeRepository, departmentRepository, null, null);
    }

    public EmployeeService(EmployeeRepository employeeRepository,
                           DepartmentRepository departmentRepository,
                           AuditService auditService) {
        this(employeeRepository, departmentRepository, auditService, null);
    }

    @Autowired
    public EmployeeService(EmployeeRepository employeeRepository,
                           DepartmentRepository departmentRepository,
                           AuditService auditService,
                           @Autowired(required = false) AccountProvisioningService accountProvisioningService) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.auditService = auditService;
        this.accountProvisioningService = accountProvisioningService;
    }

    @Transactional(readOnly = true)
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAllWithDetails();
    }

    @Transactional(readOnly = true)
    public List<Employee> getFilteredEmployees(Long departmentId, EmploymentStatus status) {
        return employeeRepository.findFiltered(departmentId, status);
    }

    @Transactional(readOnly = true)
    public Employee getEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<Employee> getEligibleManagers(Long excludeEmployeeId) {
        return employeeRepository.findEligibleManagers(excludeEmployeeId);
    }

    @Transactional(readOnly = true)
    public long countActiveEmployees() {
        return employeeRepository.countByEmploymentStatus(EmploymentStatus.ACTIVE);
    }

    @Transactional
    public Employee createEmployee(EmployeeDTO dto) {
        String trimmedEmployeeId = dto.getEmployeeId().trim();
        String trimmedEmail = dto.getEmail().trim();

        if (employeeRepository.existsByEmployeeIdIgnoreCase(trimmedEmployeeId)) {
            throw new DuplicateResourceException("Employee ID '" + trimmedEmployeeId + "' is already registered.");
        }

        if (employeeRepository.existsByEmailIgnoreCase(trimmedEmail)) {
            throw new DuplicateResourceException("Email address '" + trimmedEmail + "' is already registered.");
        }

        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Selected department not found with ID: " + dto.getDepartmentId()));

        Employee reportingManager = null;
        if (dto.getReportingManagerId() != null) {
            reportingManager = employeeRepository.findById(dto.getReportingManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Selected reporting manager not found with ID: " + dto.getReportingManagerId()));
        }

        Employee employee = new Employee();
        employee.setEmployeeId(trimmedEmployeeId);
        employee.setFullName(dto.getFullName().trim());
        employee.setEmail(trimmedEmail);
        employee.setDepartment(department);
        employee.setEmploymentType(dto.getEmploymentType());
        employee.setJoiningDate(dto.getJoiningDate());
        employee.setEmploymentStatus(dto.getEmploymentStatus() != null ? dto.getEmploymentStatus() : EmploymentStatus.ACTIVE);
        employee.setReportingManager(reportingManager);

        Employee saved = employeeRepository.save(employee);

        if (accountProvisioningService != null) {
            accountProvisioningService.createAccountForEmployee(saved, Role.ROLE_EMPLOYEE);
        }

        if (auditService != null) {
            Long deptId = department != null ? department.getId() : null;
            auditService.recordEvent("HR_ADMIN", "EMPLOYEE_CREATED", "Employee", saved.getEmployeeId(), deptId,
                    "Created employee profile for " + saved.getFullName() + " (" + saved.getEmployeeId() +
                    ") in department " + department.getName());
        }
        return saved;
    }

    @Transactional
    public Employee updateEmployee(Long id, EmployeeDTO dto) {
        Employee employee = getEmployeeById(id);

        String trimmedEmployeeId = dto.getEmployeeId().trim();
        String trimmedEmail = dto.getEmail().trim();

        if (employeeRepository.existsByEmployeeIdIgnoreCaseAndIdNot(trimmedEmployeeId, id)) {
            throw new DuplicateResourceException("Employee ID '" + trimmedEmployeeId + "' is already in use by another employee.");
        }

        if (employeeRepository.existsByEmailIgnoreCaseAndIdNot(trimmedEmail, id)) {
            throw new DuplicateResourceException("Email address '" + trimmedEmail + "' is already in use by another employee.");
        }

        if (dto.getReportingManagerId() != null && dto.getReportingManagerId().equals(id)) {
            throw new IllegalArgumentException("An employee cannot be their own reporting manager.");
        }

        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Selected department not found with ID: " + dto.getDepartmentId()));

        Employee reportingManager = null;
        if (dto.getReportingManagerId() != null) {
            reportingManager = employeeRepository.findById(dto.getReportingManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Selected reporting manager not found with ID: " + dto.getReportingManagerId()));
        }

        employee.setEmployeeId(trimmedEmployeeId);
        employee.setFullName(dto.getFullName().trim());
        employee.setEmail(trimmedEmail);
        employee.setDepartment(department);
        employee.setEmploymentType(dto.getEmploymentType());
        employee.setJoiningDate(dto.getJoiningDate());
        employee.setEmploymentStatus(dto.getEmploymentStatus());
        employee.setReportingManager(reportingManager);

        Employee updated = employeeRepository.save(employee);
        if (auditService != null) {
            Long deptId = department != null ? department.getId() : null;
            auditService.recordEvent("HR_ADMIN", "EMPLOYEE_UPDATED", "Employee", updated.getEmployeeId(), deptId,
                    "Updated employee profile for " + updated.getFullName() + " (" + updated.getEmployeeId() + ")");
        }
        return updated;
    }

    @Transactional
    public Employee toggleEmployeeStatus(Long id) {
        Employee employee = getEmployeeById(id);
        EmploymentStatus oldStatus = employee.getEmploymentStatus();
        EmploymentStatus newStatus = (oldStatus == EmploymentStatus.ACTIVE)
                ? EmploymentStatus.INACTIVE
                : EmploymentStatus.ACTIVE;
        employee.setEmploymentStatus(newStatus);
        Employee updated = employeeRepository.save(employee);

        if (auditService != null) {
            Long deptId = updated.getDepartment() != null ? updated.getDepartment().getId() : null;
            auditService.recordEvent("HR_ADMIN", "EMPLOYEE_STATUS_TOGGLED", "Employee", updated.getEmployeeId(), deptId,
                    "Toggled employment status for " + updated.getFullName() + " (" + updated.getEmployeeId() +
                    ") from " + oldStatus + " to " + newStatus);
        }
        return updated;
    }

    @Transactional
    public Employee toggleEmploymentStatus(Long id) {
        return toggleEmployeeStatus(id);
    }

    public EmployeeDTO convertToDTO(Employee employee) {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setEmployeeId(employee.getEmployeeId());
        dto.setFullName(employee.getFullName());
        dto.setEmail(employee.getEmail());
        dto.setDepartmentId(employee.getDepartment().getId());
        dto.setEmploymentType(employee.getEmploymentType());
        dto.setJoiningDate(employee.getJoiningDate());
        dto.setEmploymentStatus(employee.getEmploymentStatus());
        if (employee.getReportingManager() != null) {
            dto.setReportingManagerId(employee.getReportingManager().getId());
        }
        return dto;
    }
}
