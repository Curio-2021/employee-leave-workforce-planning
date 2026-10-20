package com.anudip.employee_leave.repository;

import com.anudip.employee_leave.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    boolean existsByNameIgnoreCaseAndHolidayDate(String name, LocalDate holidayDate);

    boolean existsByNameIgnoreCaseAndHolidayDateAndIdNot(String name, LocalDate holidayDate, Long id);

    List<Holiday> findAllByOrderByHolidayDateAsc();

    List<Holiday> findByCalendarYearOrderByHolidayDateAsc(int calendarYear);

    List<Holiday> findByActiveTrueAndHolidayDateBetweenOrderByHolidayDateAsc(LocalDate startDate, LocalDate endDate);

    boolean existsByActiveTrueAndHolidayDate(LocalDate holidayDate);
}

