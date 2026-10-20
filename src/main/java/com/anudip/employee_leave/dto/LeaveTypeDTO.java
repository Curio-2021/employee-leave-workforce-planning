package com.anudip.employee_leave.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class LeaveTypeDTO {

    private Long id;

    @NotBlank(message = "Leave type name is required")
    @Size(min = 2, max = 100, message = "Leave type name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Leave type code is required")
    @Size(min = 2, max = 10, message = "Leave type code must be between 2 and 10 characters")
    @Pattern(regexp = "^[A-Za-z0-9_-]{2,10}$", message = "Code must be 2-10 alphanumeric characters")
    private String code;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    private boolean paid = true;

    private boolean active = true;

    @Min(value = 1, message = "Max consecutive days must be at least 1 day")
    @Max(value = 180, message = "Max consecutive days cannot exceed 180 days")
    private Integer maxConsecutiveDays;

    private boolean requiresAttachment = false;

    public LeaveTypeDTO() {
    }

    public LeaveTypeDTO(String name, String code, String description, boolean paid, boolean active,
                        Integer maxConsecutiveDays, boolean requiresAttachment) {
        this.name = name;
        this.code = code;
        this.description = description;
        this.paid = paid;
        this.active = active;
        this.maxConsecutiveDays = maxConsecutiveDays;
        this.requiresAttachment = requiresAttachment;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Integer getMaxConsecutiveDays() {
        return maxConsecutiveDays;
    }

    public void setMaxConsecutiveDays(Integer maxConsecutiveDays) {
        this.maxConsecutiveDays = maxConsecutiveDays;
    }

    public boolean isRequiresAttachment() {
        return requiresAttachment;
    }

    public void setRequiresAttachment(boolean requiresAttachment) {
        this.requiresAttachment = requiresAttachment;
    }
}

