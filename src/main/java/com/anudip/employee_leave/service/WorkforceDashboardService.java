package com.anudip.employee_leave.service;

import com.anudip.employee_leave.dto.DailyAvailabilitySnapshot;
import com.anudip.employee_leave.dto.DashboardSummaryDTO;
import com.anudip.employee_leave.dto.DashboardSummaryDTO.DepartmentWorkforceStat;
import com.anudip.employee_leave.dto.DashboardSummaryDTO.UpcomingLeaveItem;
import com.anudip.employee_leave.entity.Department;
import com.anudip.employee_leave.entity.EmploymentStatus;
import com.anudip.employee_leave.entity.LeaveRequest;
import com.anudip.employee_leave.entity.LeaveRequestStatus;
import com.anudip.employee_leave.repository.DepartmentRepository;
import com.anudip.employee_leave.repository.EmployeeRepository;
import com.anudip.employee_leave.repository.LeaveRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class WorkforceDashboardService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final WorkforceAvailabilityService workforceAvailabilityService;
    private final PolicyExceptionService policyExceptionService;
    private final AuditService auditService;

    public WorkforceDashboardService(EmployeeRepository employeeRepository,
                                     DepartmentRepository departmentRepository,
                                     LeaveRequestRepository leaveRequestRepository,
                                     WorkforceAvailabilityService workforceAvailabilityService,
                                     PolicyExceptionService policyExceptionService,
                                     AuditService auditService) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.workforceAvailabilityService = workforceAvailabilityService;
        this.policyExceptionService = policyExceptionService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryDTO getDashboardSummary() {
        return getDashboardSummary(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public DashboardSummaryDTO getDashboardSummary(LocalDate referenceDate) {
        if (referenceDate == null) {
            referenceDate = LocalDate.now();
        }

        DashboardSummaryDTO dto = new DashboardSummaryDTO();

        // High-level counts
        dto.setTotalEmployees(employeeRepository.count());
        dto.setActiveEmployees(employeeRepository.countByEmploymentStatus(EmploymentStatus.ACTIVE));
        dto.setTotalDepartments(departmentRepository.count());
        dto.setPendingRequestsCount(leaveRequestRepository.countByStatus(LeaveRequestStatus.PENDING));
        dto.setApprovedRequestsCount(leaveRequestRepository.countByStatus(LeaveRequestStatus.APPROVED));
        dto.setRejectedRequestsCount(leaveRequestRepository.countByStatus(LeaveRequestStatus.REJECTED));

        // Department-by-department availability analysis for today
        List<Department> departments = departmentRepository.findAllByOrderByNameAsc();
        List<DepartmentWorkforceStat> stats = new ArrayList<>();
        double totalOnLeaveToday = 0.0;
        int shortageDeptsCount = 0;

        for (Department dept : departments) {
            DailyAvailabilitySnapshot snapshot = workforceAvailabilityService.getDepartmentDailyAvailability(dept.getId(), referenceDate);
            boolean isShortage = snapshot.getDeficit() > 0;
            if (isShortage) {
                shortageDeptsCount++;
            }
            totalOnLeaveToday += snapshot.getApprovedAbsences();

            DepartmentWorkforceStat stat = new DepartmentWorkforceStat(
                    dept.getId(),
                    dept.getName(),
                    snapshot.getTotalActiveStaff(),
                    snapshot.getRequiredStaff(),
                    snapshot.getApprovedAbsences(),
                    snapshot.getAvailableStaff(),
                    isShortage,
                    snapshot.getDeficit()
            );
            stats.add(stat);
        }

        dto.setDepartmentStats(stats);
        dto.setEmployeesOnLeaveToday(totalOnLeaveToday);
        dto.setDepartmentsWithShortageCount(shortageDeptsCount);

        // Upcoming leaves in 14-day window: [referenceDate, referenceDate + 14]
        LocalDate windowEnd = referenceDate.plusDays(14);
        List<LeaveRequest> upcomingApproved = leaveRequestRepository.findLeavesInDateRangeWithDetails(
                referenceDate, windowEnd, LeaveRequestStatus.APPROVED);

        List<UpcomingLeaveItem> upcomingItems = new ArrayList<>();
        for (LeaveRequest req : upcomingApproved) {
            UpcomingLeaveItem item = new UpcomingLeaveItem(
                    req.getId(),
                    req.getRequestNumber(),
                    req.getEmployee() != null ? req.getEmployee().getFullName() : "Unknown",
                    req.getEmployee() != null ? req.getEmployee().getEmployeeId() : "—",
                    (req.getEmployee() != null && req.getEmployee().getDepartment() != null)
                            ? req.getEmployee().getDepartment().getName() : "Unassigned",
                    req.getLeaveType() != null ? req.getLeaveType().getName() : "Leave",
                    req.getStartDate(),
                    req.getEndDate(),
                    req.getDuration() != null ? req.getDuration().doubleValue() : 0.0,
                    req.isHalfDay(),
                    req.isHalfDay() ? "HALF_DAY" : null
            );
            upcomingItems.add(item);
        }
        dto.setUpcomingLeaves(upcomingItems);

        // Action queue (pending requests needing approval)
        List<LeaveRequest> pendingQueue = leaveRequestRepository.findByStatusOrderBySubmissionTimestampAsc(LeaveRequestStatus.PENDING);
        dto.setPendingActionQueue(pendingQueue);

        // Audit events & Exception counts
        if (auditService != null) {
            dto.setRecentAuditEvents(auditService.getRecentEvents());
        }
        if (policyExceptionService != null) {
            dto.setExceptionCounts(policyExceptionService.getExceptionCounts());
        }

        return dto;
    }
}
