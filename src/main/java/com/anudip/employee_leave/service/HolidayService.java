package com.anudip.employee_leave.service;

import com.anudip.employee_leave.dto.HolidayDTO;
import com.anudip.employee_leave.entity.Holiday;
import com.anudip.employee_leave.exception.DuplicateResourceException;
import com.anudip.employee_leave.exception.ResourceNotFoundException;
import com.anudip.employee_leave.repository.HolidayRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class HolidayService {

    private final HolidayRepository holidayRepository;

    public HolidayService(HolidayRepository holidayRepository) {
        this.holidayRepository = holidayRepository;
    }

    @Transactional(readOnly = true)
    public List<Holiday> getAllHolidays() {
        return holidayRepository.findAllByOrderByHolidayDateAsc();
    }

    @Transactional(readOnly = true)
    public List<Holiday> getHolidaysByYear(int year) {
        return holidayRepository.findByCalendarYearOrderByHolidayDateAsc(year);
    }

    @Transactional(readOnly = true)
    public Holiday getHolidayById(Long id) {
        return holidayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public boolean isHoliday(LocalDate date) {
        return holidayRepository.existsByActiveTrueAndHolidayDate(date);
    }

    @Transactional
    public Holiday createHoliday(HolidayDTO dto) {
        String trimmedName = dto.getName().trim();
        LocalDate date = dto.getHolidayDate();

        if (holidayRepository.existsByNameIgnoreCaseAndHolidayDate(trimmedName, date)) {
            throw new DuplicateResourceException("Holiday '" + trimmedName + "' on " + date + " already exists.");
        }

        Holiday holiday = new Holiday();
        holiday.setName(trimmedName);
        holiday.setHolidayDate(date);
        holiday.setCalendarYear(date.getYear());
        holiday.setHolidayType(dto.getHolidayType());
        holiday.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        holiday.setActive(dto.isActive());

        return holidayRepository.save(holiday);
    }

    @Transactional
    public Holiday updateHoliday(Long id, HolidayDTO dto) {
        Holiday holiday = getHolidayById(id);

        String trimmedName = dto.getName().trim();
        LocalDate date = dto.getHolidayDate();

        if (holidayRepository.existsByNameIgnoreCaseAndHolidayDateAndIdNot(trimmedName, date, id)) {
            throw new DuplicateResourceException("Holiday '" + trimmedName + "' on " + date + " already exists.");
        }

        holiday.setName(trimmedName);
        holiday.setHolidayDate(date);
        holiday.setCalendarYear(date.getYear());
        holiday.setHolidayType(dto.getHolidayType());
        holiday.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        holiday.setActive(dto.isActive());

        return holidayRepository.save(holiday);
    }

    @Transactional
    public void toggleHolidayStatus(Long id) {
        Holiday holiday = getHolidayById(id);
        holiday.setActive(!holiday.isActive());
        holidayRepository.save(holiday);
    }

    public HolidayDTO convertToDTO(Holiday holiday) {
        HolidayDTO dto = new HolidayDTO();
        dto.setId(holiday.getId());
        dto.setName(holiday.getName());
        dto.setHolidayDate(holiday.getHolidayDate());
        dto.setHolidayType(holiday.getHolidayType());
        dto.setDescription(holiday.getDescription());
        dto.setActive(holiday.isActive());
        return dto;
    }
}

