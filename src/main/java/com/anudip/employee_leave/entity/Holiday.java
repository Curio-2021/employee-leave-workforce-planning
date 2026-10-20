package com.anudip.employee_leave.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Entity
@Table(name = "holidays")
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Holiday name is required")
    @Size(min = 2, max = 100, message = "Holiday name must be between 2 and 100 characters")
    @Column(nullable = false, length = 100)
    private String name;

    @NotNull(message = "Holiday date is required")
    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    @Column(name = "calendar_year", nullable = false)
    private int calendarYear;

    @NotNull(message = "Holiday type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "holiday_type", nullable = false, length = 30)
    private HolidayType holidayType;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    @Column(length = 255)
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public Holiday() {
    }

    public Holiday(String name, LocalDate holidayDate, HolidayType holidayType, String description, boolean active) {
        this.name = name;
        this.holidayDate = holidayDate;
        this.calendarYear = holidayDate != null ? holidayDate.getYear() : 0;
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
        if (holidayDate != null) {
            this.calendarYear = holidayDate.getYear();
        }
    }

    public int getCalendarYear() {
        return calendarYear;
    }

    public void setCalendarYear(int calendarYear) {
        this.calendarYear = calendarYear;
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

