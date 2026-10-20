package com.anudip.employee_leave.service;

import com.anudip.employee_leave.dto.DepartmentDTO;
import com.anudip.employee_leave.entity.Department;
import com.anudip.employee_leave.exception.DuplicateResourceException;
import com.anudip.employee_leave.exception.ResourceNotFoundException;
import com.anudip.employee_leave.repository.DepartmentRepository;
import com.anudip.employee_leave.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditService auditService;

    public DepartmentService(DepartmentRepository departmentRepository, EmployeeRepository employeeRepository) {
        this(departmentRepository, employeeRepository, null);
    }

    @Autowired
    public DepartmentService(DepartmentRepository departmentRepository,
                             EmployeeRepository employeeRepository,
                             AuditService auditService) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<Department> getAllDepartments() {
        return departmentRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Department getDepartmentById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));
    }

    @Transactional
    public Department createDepartment(DepartmentDTO dto) {
        String trimmedName = dto.getName().trim();
        if (departmentRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new DuplicateResourceException("Department with name '" + trimmedName + "' already exists.");
        }

        Department department = new Department();
        department.setName(trimmedName);
        department.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);

        Department saved = departmentRepository.save(department);
        if (auditService != null) {
            auditService.recordEvent("HR_ADMIN", "DEPARTMENT_CREATED", "Department", "DEPT-" + saved.getId(), saved.getId(),
                    "Created department '" + saved.getName() + "'");
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public long countEmployeesInDepartment(Long departmentId) {
        return employeeRepository.countByDepartmentId(departmentId);
    }
}

