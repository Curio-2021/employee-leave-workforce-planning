package com.anudip.employee_leave.service;

import com.anudip.employee_leave.dto.DepartmentStaffingRuleDTO;
import com.anudip.employee_leave.entity.Department;
import com.anudip.employee_leave.entity.DepartmentStaffingRule;
import com.anudip.employee_leave.exception.ResourceNotFoundException;
import com.anudip.employee_leave.repository.DepartmentRepository;
import com.anudip.employee_leave.repository.DepartmentStaffingRuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DepartmentStaffingRuleService {

    private final DepartmentStaffingRuleRepository staffingRuleRepository;
    private final DepartmentRepository departmentRepository;
    private final AuditService auditService;

    public DepartmentStaffingRuleService(DepartmentStaffingRuleRepository staffingRuleRepository,
                                         DepartmentRepository departmentRepository) {
        this(staffingRuleRepository, departmentRepository, null);
    }

    @Autowired
    public DepartmentStaffingRuleService(DepartmentStaffingRuleRepository staffingRuleRepository,
                                         DepartmentRepository departmentRepository,
                                         AuditService auditService) {
        this.staffingRuleRepository = staffingRuleRepository;
        this.departmentRepository = departmentRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<DepartmentStaffingRuleDTO> getAllDepartmentRules() {
        List<Department> departments = departmentRepository.findAllByOrderByNameAsc();
        List<DepartmentStaffingRule> existingRules = staffingRuleRepository.findAllWithDepartment();

        Map<Long, DepartmentStaffingRule> ruleMap = existingRules.stream()
                .collect(Collectors.toMap(r -> r.getDepartment().getId(), r -> r));

        List<DepartmentStaffingRuleDTO> result = new ArrayList<>();
        for (Department dept : departments) {
            DepartmentStaffingRule rule = ruleMap.get(dept.getId());
            DepartmentStaffingRuleDTO dto = new DepartmentStaffingRuleDTO();
            dto.setDepartmentId(dept.getId());
            dto.setDepartmentName(dept.getName());

            if (rule != null) {
                dto.setId(rule.getId());
                dto.setMinimumStaffCount(rule.getMinimumStaffCount());
                dto.setMinimumStaffPercentage(rule.getMinimumStaffPercentage());
                dto.setStrictEnforcement(rule.isStrictEnforcement());
                dto.setIncludeWeekends(rule.isIncludeWeekends());
                dto.setActive(rule.isActive());
                dto.setDescription(rule.getDescription());
            } else {
                // Default unconfigured baseline
                dto.setMinimumStaffCount(1);
                dto.setMinimumStaffPercentage(null);
                dto.setStrictEnforcement(false);
                dto.setIncludeWeekends(false);
                dto.setActive(false);
                dto.setDescription("Unconfigured (Default 1 staff baseline applied)");
            }
            result.add(dto);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public DepartmentStaffingRuleDTO getRuleDTOByDepartmentId(Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + departmentId));

        Optional<DepartmentStaffingRule> ruleOpt = staffingRuleRepository.findByDepartmentId(departmentId);
        DepartmentStaffingRuleDTO dto = new DepartmentStaffingRuleDTO();
        dto.setDepartmentId(department.getId());
        dto.setDepartmentName(department.getName());

        if (ruleOpt.isPresent()) {
            DepartmentStaffingRule rule = ruleOpt.get();
            dto.setId(rule.getId());
            dto.setMinimumStaffCount(rule.getMinimumStaffCount());
            dto.setMinimumStaffPercentage(rule.getMinimumStaffPercentage());
            dto.setStrictEnforcement(rule.isStrictEnforcement());
            dto.setIncludeWeekends(rule.isIncludeWeekends());
            dto.setActive(rule.isActive());
            dto.setDescription(rule.getDescription());
        } else {
            dto.setMinimumStaffCount(1);
            dto.setMinimumStaffPercentage(null);
            dto.setStrictEnforcement(false);
            dto.setIncludeWeekends(false);
            dto.setActive(true);
            dto.setDescription("");
        }
        return dto;
    }

    @Transactional
    public DepartmentStaffingRule saveOrUpdateRule(DepartmentStaffingRuleDTO dto) {
        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + dto.getDepartmentId()));

        DepartmentStaffingRule rule = staffingRuleRepository.findByDepartmentId(dto.getDepartmentId())
                .orElseGet(() -> {
                    DepartmentStaffingRule newRule = new DepartmentStaffingRule();
                    newRule.setDepartment(department);
                    return newRule;
                });

        rule.setMinimumStaffCount(dto.getMinimumStaffCount());
        rule.setMinimumStaffPercentage(dto.getMinimumStaffPercentage());
        rule.setStrictEnforcement(dto.isStrictEnforcement());
        rule.setIncludeWeekends(dto.isIncludeWeekends());
        rule.setActive(dto.isActive());
        rule.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);

        DepartmentStaffingRule saved = staffingRuleRepository.save(rule);
        if (auditService != null) {
            auditService.recordEvent("HR_ADMIN", "STAFFING_RULE_SAVED", "DepartmentStaffingRule",
                    "RULE-" + saved.getId(), department.getId(),
                    "Configured staffing rule for department '" + department.getName() + "': minCount=" +
                    saved.getMinimumStaffCount() + ", minPct=" + saved.getMinimumStaffPercentage() +
                    ", strict=" + saved.isStrictEnforcement() + ", includeWeekends=" + saved.isIncludeWeekends());
        }
        return saved;
    }
}

