package com.anudip.employee_leave.service;

import com.anudip.employee_leave.dto.LeavePolicyDTO;
import com.anudip.employee_leave.entity.Department;
import com.anudip.employee_leave.entity.LeavePolicy;
import com.anudip.employee_leave.entity.LeaveType;
import com.anudip.employee_leave.exception.DuplicateResourceException;
import com.anudip.employee_leave.exception.ResourceNotFoundException;
import com.anudip.employee_leave.repository.DepartmentRepository;
import com.anudip.employee_leave.repository.LeavePolicyRepository;
import com.anudip.employee_leave.repository.LeaveTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LeavePolicyService {

    private final LeavePolicyRepository leavePolicyRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final DepartmentRepository departmentRepository;
    private final AuditService auditService;

    public LeavePolicyService(LeavePolicyRepository leavePolicyRepository,
                              LeaveTypeRepository leaveTypeRepository,
                              DepartmentRepository departmentRepository) {
        this(leavePolicyRepository, leaveTypeRepository, departmentRepository, null);
    }

    @Autowired
    public LeavePolicyService(LeavePolicyRepository leavePolicyRepository,
                              LeaveTypeRepository leaveTypeRepository,
                              DepartmentRepository departmentRepository,
                              AuditService auditService) {
        this.leavePolicyRepository = leavePolicyRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.departmentRepository = departmentRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<LeavePolicy> getAllPolicies() {
        return leavePolicyRepository.findAllWithDetails();
    }

    @Transactional(readOnly = true)
    public LeavePolicy getPolicyById(Long id) {
        return leavePolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave policy not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<LeavePolicy> getPoliciesByYear(int year) {
        return leavePolicyRepository.findByEffectiveYear(year);
    }

    @Transactional
    public LeavePolicy createPolicy(LeavePolicyDTO dto) {
        String trimmedName = dto.getPolicyName().trim();
        int year = dto.getEffectiveYear();

        if (leavePolicyRepository.existsByPolicyNameIgnoreCaseAndEffectiveYear(trimmedName, year)) {
            throw new DuplicateResourceException("Policy '" + trimmedName + "' already exists for year " + year + ".");
        }

        LeaveType leaveType = leaveTypeRepository.findById(dto.getLeaveTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Selected leave type not found with ID: " + dto.getLeaveTypeId()));

        Department department = null;
        if (dto.getDepartmentId() != null) {
            department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Selected department not found with ID: " + dto.getDepartmentId()));
        }

        LeavePolicy policy = new LeavePolicy();
        policy.setPolicyName(trimmedName);
        policy.setLeaveType(leaveType);
        policy.setApplicableDepartment(department);
        policy.setApplicableEmploymentType(dto.getApplicableEmploymentType());
        policy.setAnnualAllocation(dto.getAnnualAllocation());
        policy.setAllowCarryForward(dto.isAllowCarryForward());
        policy.setMaxCarryForwardDays(dto.isAllowCarryForward() ? dto.getMaxCarryForwardDays() : 0);
        policy.setMinServiceDays(dto.getMinServiceDays());
        policy.setEffectiveYear(year);
        policy.setActive(dto.isActive());

        LeavePolicy saved = leavePolicyRepository.save(policy);
        if (auditService != null) {
            Long deptId = department != null ? department.getId() : null;
            auditService.recordEvent("HR_ADMIN", "POLICY_CREATED", "LeavePolicy", "POL-" + saved.getId(), deptId,
                    "Created leave policy '" + saved.getPolicyName() + "' (Allocation: " + saved.getAnnualAllocation() +
                    " days, Year: " + saved.getEffectiveYear() + ")");
        }
        return saved;
    }

    @Transactional
    public LeavePolicy updatePolicy(Long id, LeavePolicyDTO dto) {
        LeavePolicy policy = getPolicyById(id);

        String trimmedName = dto.getPolicyName().trim();
        int year = dto.getEffectiveYear();

        if (leavePolicyRepository.existsByPolicyNameIgnoreCaseAndEffectiveYearAndIdNot(trimmedName, year, id)) {
            throw new DuplicateResourceException("Policy '" + trimmedName + "' already exists for year " + year + ".");
        }

        LeaveType leaveType = leaveTypeRepository.findById(dto.getLeaveTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Selected leave type not found with ID: " + dto.getLeaveTypeId()));

        Department department = null;
        if (dto.getDepartmentId() != null) {
            department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Selected department not found with ID: " + dto.getDepartmentId()));
        }

        policy.setPolicyName(trimmedName);
        policy.setLeaveType(leaveType);
        policy.setApplicableDepartment(department);
        policy.setApplicableEmploymentType(dto.getApplicableEmploymentType());
        policy.setAnnualAllocation(dto.getAnnualAllocation());
        policy.setAllowCarryForward(dto.isAllowCarryForward());
        policy.setMaxCarryForwardDays(dto.isAllowCarryForward() ? dto.getMaxCarryForwardDays() : 0);
        policy.setMinServiceDays(dto.getMinServiceDays());
        policy.setEffectiveYear(year);
        policy.setActive(dto.isActive());

        LeavePolicy saved = leavePolicyRepository.save(policy);
        if (auditService != null) {
            Long deptId = department != null ? department.getId() : null;
            auditService.recordEvent("HR_ADMIN", "POLICY_UPDATED", "LeavePolicy", "POL-" + saved.getId(), deptId,
                    "Updated leave policy '" + saved.getPolicyName() + "' (Allocation: " + saved.getAnnualAllocation() +
                    " days, Year: " + saved.getEffectiveYear() + ")");
        }
        return saved;
    }

    @Transactional
    public void togglePolicyStatus(Long id) {
        LeavePolicy policy = getPolicyById(id);
        policy.setActive(!policy.isActive());
        LeavePolicy saved = leavePolicyRepository.save(policy);
        if (auditService != null) {
            Long deptId = saved.getApplicableDepartment() != null ? saved.getApplicableDepartment().getId() : null;
            auditService.recordEvent("HR_ADMIN", "POLICY_STATUS_TOGGLED", "LeavePolicy", "POL-" + saved.getId(), deptId,
                    "Toggled status of leave policy '" + saved.getPolicyName() + "' to " + (saved.isActive() ? "ACTIVE" : "INACTIVE"));
        }
    }

    public LeavePolicyDTO convertToDTO(LeavePolicy policy) {
        LeavePolicyDTO dto = new LeavePolicyDTO();
        dto.setId(policy.getId());
        dto.setPolicyName(policy.getPolicyName());
        dto.setLeaveTypeId(policy.getLeaveType() != null ? policy.getLeaveType().getId() : null);
        dto.setDepartmentId(policy.getApplicableDepartment() != null ? policy.getApplicableDepartment().getId() : null);
        dto.setApplicableEmploymentType(policy.getApplicableEmploymentType());
        dto.setAnnualAllocation(policy.getAnnualAllocation());
        dto.setAllowCarryForward(policy.isAllowCarryForward());
        dto.setMaxCarryForwardDays(policy.getMaxCarryForwardDays());
        dto.setMinServiceDays(policy.getMinServiceDays());
        dto.setEffectiveYear(policy.getEffectiveYear());
        dto.setActive(policy.isActive());
        return dto;
    }
}

