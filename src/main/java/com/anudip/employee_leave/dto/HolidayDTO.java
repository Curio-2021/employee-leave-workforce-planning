package com.anudip.employee_leave.dto;

import com.anudip.employee_leave.entity.HolidayType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class HolidayDTO {

    private Long id;

    @NotBlank(message = "Holiday name is required")
    @Size(min = 2, max = 100, message = "Holiday name must be between 2 and 100 characters")
    private String name;

    @NotNull(message = "Holiday date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate holidayDate;

    @NotNull(message = "Holiday type is required")
    private HolidayType holidayType = HolidayType.MANDATORY;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    private boolean active = true;

    public HolidayDTO() {
    }

    public HolidayDTO(String name, LocalDate holidayDate, HolidayType holidayType, String description, boolean active) {
        this.name = name;
        this.holidayDate = holidayDate;
        this.holidayType = holidayType;
        this.description = description;
        this.active = active;
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

    public LocalDate getHolidayDate() {
        return holidayDate;
    }

    public void setHolidayDate(LocalDate holidayDate) {
        this.holidayDate = holidayDate;
    }

    public HolidayType getHolidayType() {
        return holidayType;
    }

    public void setHolidayType(HolidayType holidayType) {
        this.holidayType = holidayType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

