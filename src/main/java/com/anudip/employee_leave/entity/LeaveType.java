package com.anudip.employee_leave.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "leave_types")
public class LeaveType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Leave type name is required")
    @Size(min = 2, max = 100, message = "Leave type name must be between 2 and 100 characters")
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @NotBlank(message = "Leave type code is required")
    @Size(min = 2, max = 10, message = "Leave type code must be between 2 and 10 characters")
    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    @Column(length = 255)
    private String description;

    @Column(name = "is_paid", nullable = false)
    private boolean paid = true;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "max_consecutive_days")
    private Integer maxConsecutiveDays;

    @Column(name = "requires_attachment", nullable = false)
    private boolean requiresAttachment = false;

    // Do NOT cascade remove to prevent accidental deletion of policies/leaves
    @OneToMany(mappedBy = "leaveType")
    private List<LeavePolicy> policies = new ArrayList<>();

    public LeaveType() {
    }

    public LeaveType(String name, String code, String description, boolean paid, boolean active,
                     Integer maxConsecutiveDays, boolean requiresAttachment) {
        this.name = name;
        this.code = code != null ? code.toUpperCase() : null;
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
        this.code = code != null ? code.toUpperCase() : null;
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

    public List<LeavePolicy> getPolicies() {
        return policies;
    }

    public void setPolicies(List<LeavePolicy> policies) {
        this.policies = policies;
    }
}

