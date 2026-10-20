package com.anudip.employee_leave.service;

import com.anudip.employee_leave.dto.LeaveTypeDTO;
import com.anudip.employee_leave.entity.LeaveType;
import com.anudip.employee_leave.exception.DuplicateResourceException;
import com.anudip.employee_leave.exception.ResourceNotFoundException;
import com.anudip.employee_leave.repository.LeaveTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LeaveTypeService {

    private final LeaveTypeRepository leaveTypeRepository;

    public LeaveTypeService(LeaveTypeRepository leaveTypeRepository) {
        this.leaveTypeRepository = leaveTypeRepository;
    }

    @Transactional(readOnly = true)
    public List<LeaveType> getAllLeaveTypes() {
        return leaveTypeRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<LeaveType> getActiveLeaveTypes() {
        return leaveTypeRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public LeaveType getLeaveTypeById(Long id) {
        return leaveTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave type not found with ID: " + id));
    }

    @Transactional
    public LeaveType createLeaveType(LeaveTypeDTO dto) {
        String trimmedName = dto.getName().trim();
        String trimmedCode = dto.getCode().trim().toUpperCase();

        if (leaveTypeRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new DuplicateResourceException("Leave type with name '" + trimmedName + "' already exists.");
        }

        if (leaveTypeRepository.existsByCodeIgnoreCase(trimmedCode)) {
            throw new DuplicateResourceException("Leave type with code '" + trimmedCode + "' already exists.");
        }

        LeaveType leaveType = new LeaveType();
        leaveType.setName(trimmedName);
        leaveType.setCode(trimmedCode);
        leaveType.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        leaveType.setPaid(dto.isPaid());
        leaveType.setActive(dto.isActive());
        leaveType.setMaxConsecutiveDays(dto.getMaxConsecutiveDays());
        leaveType.setRequiresAttachment(dto.isRequiresAttachment());

        return leaveTypeRepository.save(leaveType);
    }

    @Transactional
    public LeaveType updateLeaveType(Long id, LeaveTypeDTO dto) {
        LeaveType leaveType = getLeaveTypeById(id);

        String trimmedName = dto.getName().trim();
        String trimmedCode = dto.getCode().trim().toUpperCase();

        if (leaveTypeRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
            throw new DuplicateResourceException("Leave type with name '" + trimmedName + "' already exists.");
        }

        if (leaveTypeRepository.existsByCodeIgnoreCaseAndIdNot(trimmedCode, id)) {
            throw new DuplicateResourceException("Leave type with code '" + trimmedCode + "' already exists.");
        }

        leaveType.setName(trimmedName);
        leaveType.setCode(trimmedCode);
        leaveType.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        leaveType.setPaid(dto.isPaid());
        leaveType.setActive(dto.isActive());
        leaveType.setMaxConsecutiveDays(dto.getMaxConsecutiveDays());
        leaveType.setRequiresAttachment(dto.isRequiresAttachment());

        return leaveTypeRepository.save(leaveType);
    }

    @Transactional
    public void toggleLeaveTypeStatus(Long id) {
        LeaveType leaveType = getLeaveTypeById(id);
        leaveType.setActive(!leaveType.isActive());
        leaveTypeRepository.save(leaveType);
    }

    public LeaveTypeDTO convertToDTO(LeaveType leaveType) {
        LeaveTypeDTO dto = new LeaveTypeDTO();
        dto.setId(leaveType.getId());
        dto.setName(leaveType.getName());
        dto.setCode(leaveType.getCode());
        dto.setDescription(leaveType.getDescription());
        dto.setPaid(leaveType.isPaid());
        dto.setActive(leaveType.isActive());
        dto.setMaxConsecutiveDays(leaveType.getMaxConsecutiveDays());
        dto.setRequiresAttachment(leaveType.isRequiresAttachment());
        return dto;
    }
}

