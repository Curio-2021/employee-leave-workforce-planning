package com.anudip.employee_leave.service;

import com.anudip.employee_leave.dto.HolidayDTO;
import com.anudip.employee_leave.entity.Holiday;
import com.anudip.employee_leave.entity.HolidayType;
import com.anudip.employee_leave.exception.DuplicateResourceException;
import com.anudip.employee_leave.repository.HolidayRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HolidayServiceTest {

    @Mock
    private HolidayRepository holidayRepository;

    @InjectMocks
    private HolidayService holidayService;

    private Holiday holiday;

    @BeforeEach
    void setUp() {
        holiday = new Holiday("Republic Day", LocalDate.of(2026, 1, 26), HolidayType.MANDATORY, "National holiday", true);
        holiday.setId(1L);
    }

    @Test
    void testCreateHolidaySuccess() {
        HolidayDTO dto = new HolidayDTO("Republic Day", LocalDate.of(2026, 1, 26), HolidayType.MANDATORY, "Desc", true);

        when(holidayRepository.existsByNameIgnoreCaseAndHolidayDate("Republic Day", LocalDate.of(2026, 1, 26))).thenReturn(false);
        when(holidayRepository.save(any(Holiday.class))).thenReturn(holiday);

        Holiday result = holidayService.createHoliday(dto);

        assertNotNull(result);
        assertEquals("Republic Day", result.getName());
        assertEquals(2026, result.getCalendarYear());
        verify(holidayRepository).save(any(Holiday.class));
    }

    @Test
    void testCreateHolidayDuplicateDateAndNameThrowsException() {
        HolidayDTO dto = new HolidayDTO("Republic Day", LocalDate.of(2026, 1, 26), HolidayType.MANDATORY, "Desc", true);

        when(holidayRepository.existsByNameIgnoreCaseAndHolidayDate("Republic Day", LocalDate.of(2026, 1, 26))).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> holidayService.createHoliday(dto));
        verify(holidayRepository, never()).save(any(Holiday.class));
    }

    @Test
    void testIsHolidayReturnsTrueWhenActive() {
        LocalDate date = LocalDate.of(2026, 1, 26);
        when(holidayRepository.existsByActiveTrueAndHolidayDate(date)).thenReturn(true);

        assertTrue(holidayService.isHoliday(date));
    }

    @Test
    void testToggleHolidayStatus() {
        when(holidayRepository.findById(1L)).thenReturn(Optional.of(holiday));

        holidayService.toggleHolidayStatus(1L);

        assertFalse(holiday.isActive());
        verify(holidayRepository).save(holiday);
    }
}

