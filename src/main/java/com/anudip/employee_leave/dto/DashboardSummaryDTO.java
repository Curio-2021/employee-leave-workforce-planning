package com.anudip.employee_leave.dto;

import com.anudip.employee_leave.entity.AuditEvent;
import com.anudip.employee_leave.entity.LeaveRequest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardSummaryDTO {

    private long totalEmployees;
    private long activeEmployees;
    private long totalDepartments;
    private long pendingRequestsCount;
    private long approvedRequestsCount;
    private long rejectedRequestsCount;
    private double employeesOnLeaveToday;
    private int departmentsWithShortageCount;

    private List<DepartmentWorkforceStat> departmentStats = new ArrayList<>();
    private List<UpcomingLeaveItem> upcomingLeaves = new ArrayList<>();
    private List<LeaveRequest> pendingActionQueue = new ArrayList<>();
    private List<AuditEvent> recentAuditEvents = new ArrayList<>();
    private Map<String, Long> exceptionCounts = new HashMap<>();

    public DashboardSummaryDTO() {
    }

    public long getTotalEmployees() {
        return totalEmployees;
    }

    public void setTotalEmployees(long totalEmployees) {
        this.totalEmployees = totalEmployees;
    }

    public long getActiveEmployees() {
        return activeEmployees;
    }

    public void setActiveEmployees(long activeEmployees) {
        this.activeEmployees = activeEmployees;
    }

    public long getTotalDepartments() {
        return totalDepartments;
    }

    public void setTotalDepartments(long totalDepartments) {
        this.totalDepartments = totalDepartments;
    }

    public long getPendingRequestsCount() {
        return pendingRequestsCount;
    }

    public void setPendingRequestsCount(long pendingRequestsCount) {
        this.pendingRequestsCount = pendingRequestsCount;
    }

    public long getApprovedRequestsCount() {
        return approvedRequestsCount;
    }

    public void setApprovedRequestsCount(long approvedRequestsCount) {
        this.approvedRequestsCount = approvedRequestsCount;
    }

    public long getRejectedRequestsCount() {
        return rejectedRequestsCount;
    }

    public void setRejectedRequestsCount(long rejectedRequestsCount) {
        this.rejectedRequestsCount = rejectedRequestsCount;
    }

    public double getEmployeesOnLeaveToday() {
        return employeesOnLeaveToday;
    }

    public void setEmployeesOnLeaveToday(double employeesOnLeaveToday) {
        this.employeesOnLeaveToday = employeesOnLeaveToday;
    }

    public int getDepartmentsWithShortageCount() {
        return departmentsWithShortageCount;
    }

    public void setDepartmentsWithShortageCount(int departmentsWithShortageCount) {
        this.departmentsWithShortageCount = departmentsWithShortageCount;
    }

    public List<DepartmentWorkforceStat> getDepartmentStats() {
        return departmentStats;
    }

    public void setDepartmentStats(List<DepartmentWorkforceStat> departmentStats) {
        this.departmentStats = departmentStats;
    }

    public List<UpcomingLeaveItem> getUpcomingLeaves() {
        return upcomingLeaves;
    }

    public void setUpcomingLeaves(List<UpcomingLeaveItem> upcomingLeaves) {
        this.upcomingLeaves = upcomingLeaves;
    }

    public List<LeaveRequest> getPendingActionQueue() {
        return pendingActionQueue;
    }

    public void setPendingActionQueue(List<LeaveRequest> pendingActionQueue) {
        this.pendingActionQueue = pendingActionQueue;
    }

    public List<AuditEvent> getRecentAuditEvents() {
        return recentAuditEvents;
    }

    public void setRecentAuditEvents(List<AuditEvent> recentAuditEvents) {
        this.recentAuditEvents = recentAuditEvents;
    }

    public Map<String, Long> getExceptionCounts() {
        return exceptionCounts;
    }

    public void setExceptionCounts(Map<String, Long> exceptionCounts) {
        this.exceptionCounts = exceptionCounts;
    }

    // Inner DTO for department staffing statistics
    public static class DepartmentWorkforceStat {
        private Long departmentId;
        private String departmentName;
        private int totalActiveStaff;
        private int requiredStaff;
        private double onLeaveToday;
        private double availableStaffToday;
        private boolean shortageToday;
        private double shortageDeficit;

        public DepartmentWorkforceStat() {
        }

        public DepartmentWorkforceStat(Long departmentId, String departmentName, int totalActiveStaff,
                                       int requiredStaff, double onLeaveToday, double availableStaffToday,
                                       boolean shortageToday, double shortageDeficit) {
            this.departmentId = departmentId;
            this.departmentName = departmentName;
            this.totalActiveStaff = totalActiveStaff;
            this.requiredStaff = requiredStaff;
            this.onLeaveToday = onLeaveToday;
            this.availableStaffToday = availableStaffToday;
            this.shortageToday = shortageToday;
            this.shortageDeficit = shortageDeficit;
        }

        public Long getDepartmentId() {
            return departmentId;
        }

        public void setDepartmentId(Long departmentId) {
            this.departmentId = departmentId;
        }

        public String getDepartmentName() {
            return departmentName;
        }

        public void setDepartmentName(String departmentName) {
            this.departmentName = departmentName;
        }

        public int getTotalActiveStaff() {
            return totalActiveStaff;
        }

        public void setTotalActiveStaff(int totalActiveStaff) {
            this.totalActiveStaff = totalActiveStaff;
        }

        public int getRequiredStaff() {
            return requiredStaff;
        }

        public void setRequiredStaff(int requiredStaff) {
            this.requiredStaff = requiredStaff;
        }

        public double getOnLeaveToday() {
            return onLeaveToday;
        }

        public void setOnLeaveToday(double onLeaveToday) {
            this.onLeaveToday = onLeaveToday;
        }

        public double getAvailableStaffToday() {
            return availableStaffToday;
        }

        public void setAvailableStaffToday(double availableStaffToday) {
            this.availableStaffToday = availableStaffToday;
        }

        public boolean isShortageToday() {
            return shortageToday;
        }

        public void setShortageToday(boolean shortageToday) {
            this.shortageToday = shortageToday;
        }

        public double getShortageDeficit() {
            return shortageDeficit;
        }

        public void setShortageDeficit(double shortageDeficit) {
            this.shortageDeficit = shortageDeficit;
        }
    }

    // Inner DTO for upcoming leaves
    public static class UpcomingLeaveItem {
        private Long requestId;
        private String requestNumber;
        private String employeeName;
        private String employeeIdentifier;
        private String departmentName;
        private String leaveTypeName;
        private LocalDate startDate;
        private LocalDate endDate;
        private double duration;
        private boolean halfDay;
        private String halfDayPeriod;

        public UpcomingLeaveItem() {
        }

        public UpcomingLeaveItem(Long requestId, String requestNumber, String employeeName,
                                 String employeeIdentifier, String departmentName, String leaveTypeName,
                                 LocalDate startDate, LocalDate endDate, double duration,
                                 boolean halfDay, String halfDayPeriod) {
            this.requestId = requestId;
            this.requestNumber = requestNumber;
            this.employeeName = employeeName;
            this.employeeIdentifier = employeeIdentifier;
            this.departmentName = departmentName;
            this.leaveTypeName = leaveTypeName;
            this.startDate = startDate;
            this.endDate = endDate;
            this.duration = duration;
            this.halfDay = halfDay;
            this.halfDayPeriod = halfDayPeriod;
        }

        public Long getRequestId() {
            return requestId;
        }

        public void setRequestId(Long requestId) {
            this.requestId = requestId;
        }

        public String getRequestNumber() {
            return requestNumber;
        }

        public void setRequestNumber(String requestNumber) {
            this.requestNumber = requestNumber;
        }

        public String getEmployeeName() {
            return employeeName;
        }

        public void setEmployeeName(String employeeName) {
            this.employeeName = employeeName;
        }

        public String getEmployeeIdentifier() {
            return employeeIdentifier;
        }

        public void setEmployeeIdentifier(String employeeIdentifier) {
            this.employeeIdentifier = employeeIdentifier;
        }

        public String getDepartmentName() {
            return departmentName;
        }

        public void setDepartmentName(String departmentName) {
            this.departmentName = departmentName;
        }

        public String getLeaveTypeName() {
            return leaveTypeName;
        }

        public void setLeaveTypeName(String leaveTypeName) {
            this.leaveTypeName = leaveTypeName;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public void setEndDate(LocalDate endDate) {
            this.endDate = endDate;
        }

        public double getDuration() {
            return duration;
        }

        public void setDuration(double duration) {
            this.duration = duration;
        }

        public boolean isHalfDay() {
            return halfDay;
        }

        public void setHalfDay(boolean halfDay) {
            this.halfDay = halfDay;
        }

        public String getHalfDayPeriod() {
            return halfDayPeriod;
        }

        public void setHalfDayPeriod(String halfDayPeriod) {
            this.halfDayPeriod = halfDayPeriod;
        }
    }
}

