package com.anudip.employee_leave.controller;

import com.anudip.employee_leave.dto.HolidayDTO;
import com.anudip.employee_leave.entity.Holiday;
import com.anudip.employee_leave.entity.HolidayType;
import com.anudip.employee_leave.exception.DuplicateResourceException;
import com.anudip.employee_leave.service.HolidayService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/holidays")
public class HolidayController {

    private final HolidayService holidayService;

    public HolidayController(HolidayService holidayService) {
        this.holidayService = holidayService;
    }

    @GetMapping
    public String listHolidays(@RequestParam(required = false) Integer year, Model model) {
        List<Holiday> holidays;
        if (year != null) {
            holidays = holidayService.getHolidaysByYear(year);
        } else {
            holidays = holidayService.getAllHolidays();
        }

        model.addAttribute("holidays", holidays);
        model.addAttribute("selectedYear", year);
        model.addAttribute("holidayTypes", HolidayType.values());
        if (!model.containsAttribute("holidayDTO")) {
            HolidayDTO dto = new HolidayDTO();
            dto.setHolidayDate(LocalDate.now());
            model.addAttribute("holidayDTO", dto);
        }
        return "holidays/list";
    }

    @PostMapping
    public String createHoliday(@Valid @ModelAttribute("holidayDTO") HolidayDTO holidayDTO,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("holidays", holidayService.getAllHolidays());
            model.addAttribute("holidayTypes", HolidayType.values());
            return "holidays/list";
        }

        try {
            holidayService.createHoliday(holidayDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Holiday '" + holidayDTO.getName() + "' scheduled on " + holidayDTO.getHolidayDate() + " added successfully.");
            return "redirect:/holidays";
        } catch (DuplicateResourceException ex) {
            bindingResult.rejectValue("name", "duplicate", ex.getMessage());
            model.addAttribute("holidays", holidayService.getAllHolidays());
            model.addAttribute("holidayTypes", HolidayType.values());
            return "holidays/list";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Holiday holiday = holidayService.getHolidayById(id);
        model.addAttribute("holidayDTO", holidayService.convertToDTO(holiday));
        model.addAttribute("holidayTypes", HolidayType.values());
        return "holidays/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateHoliday(@PathVariable Long id,
                                @Valid @ModelAttribute("holidayDTO") HolidayDTO holidayDTO,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("holidayTypes", HolidayType.values());
            return "holidays/edit";
        }

        try {
            holidayService.updateHoliday(id, holidayDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Holiday '" + holidayDTO.getName() + "' updated successfully.");
            return "redirect:/holidays";
        } catch (DuplicateResourceException ex) {
            bindingResult.rejectValue("name", "duplicate", ex.getMessage());
            model.addAttribute("holidayTypes", HolidayType.values());
            return "holidays/edit";
        }
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        holidayService.toggleHolidayStatus(id);
        redirectAttributes.addFlashAttribute("successMessage", "Holiday status updated successfully.");
        return "redirect:/holidays";
    }
}

