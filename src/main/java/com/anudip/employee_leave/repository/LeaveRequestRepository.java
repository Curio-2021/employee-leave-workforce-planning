package com.anudip.employee_leave.repository;

import com.anudip.employee_leave.entity.LeaveRequest;
import com.anudip.employee_leave.entity.LeaveRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    Optional<LeaveRequest> findByRequestNumber(String requestNumber);

    boolean existsByRequestNumber(String requestNumber);

    long countByStatus(LeaveRequestStatus status);

    @Query("SELECT r FROM LeaveRequest r " +
           "JOIN FETCH r.employee e " +
           "JOIN FETCH r.leaveType lt " +
           "LEFT JOIN FETCH e.department d " +
           "LEFT JOIN FETCH r.assignedApprover a " +
           "LEFT JOIN FETCH r.decidedBy db " +
           "WHERE r.id = :id")
    Optional<LeaveRequest> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT r FROM LeaveRequest r " +
           "JOIN FETCH r.employee e " +
           "JOIN FETCH r.leaveType lt " +
           "LEFT JOIN FETCH e.department d " +
           "LEFT JOIN FETCH r.assignedApprover a " +
           "LEFT JOIN FETCH r.decidedBy db " +
           "WHERE r.requestNumber = :requestNumber")
    Optional<LeaveRequest> findByRequestNumberWithDetails(@Param("requestNumber") String requestNumber);

    @Query("SELECT r FROM LeaveRequest r " +
           "JOIN FETCH r.employee e " +
           "JOIN FETCH r.leaveType lt " +
           "LEFT JOIN FETCH e.department d " +
           "LEFT JOIN FETCH r.assignedApprover a " +
           "WHERE (:employeeId IS NULL OR e.id = :employeeId) " +
           "AND (:departmentId IS NULL OR d.id = :departmentId) " +
           "AND (:leaveTypeId IS NULL OR lt.id = :leaveTypeId) " +
           "AND (:status IS NULL OR r.status = :status) " +
           "ORDER BY r.submissionTimestamp DESC")
    List<LeaveRequest> findFiltered(
            @Param("employeeId") Long employeeId,
            @Param("departmentId") Long departmentId,
            @Param("leaveTypeId") Long leaveTypeId,
            @Param("status") LeaveRequestStatus status);

    @Query("SELECT r FROM LeaveRequest r " +
           "JOIN FETCH r.employee e " +
           "JOIN FETCH r.leaveType lt " +
           "LEFT JOIN FETCH e.department d " +
           "LEFT JOIN FETCH r.assignedApprover a " +
           "WHERE r.status = :status " +
           "ORDER BY r.submissionTimestamp ASC")
    List<LeaveRequest> findByStatusOrderBySubmissionTimestampAsc(@Param("status") LeaveRequestStatus status);

    @Query("SELECT r FROM LeaveRequest r " +
           "WHERE r.employee.id = :employeeId " +
           "AND r.status IN :statuses " +
           "AND r.startDate <= :endDate AND r.endDate >= :startDate")
    List<LeaveRequest> findOverlappingRequests(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") List<LeaveRequestStatus> statuses);

    @Query("SELECT r FROM LeaveRequest r " +
           "WHERE r.employee.id = :employeeId " +
           "AND r.status IN :statuses " +
           "AND r.startDate <= :endDate AND r.endDate >= :startDate " +
           "AND (:excludeId IS NULL OR r.id != :excludeId)")
    List<LeaveRequest> findOverlappingRequests(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") List<LeaveRequestStatus> statuses,
            @Param("excludeId") Long excludeId);

    @Query("SELECT r FROM LeaveRequest r " +
           "JOIN FETCH r.employee e " +
           "JOIN FETCH r.leaveType lt " +
           "LEFT JOIN FETCH e.department d " +
           "WHERE e.department.id = :departmentId " +
           "AND r.status IN :statuses " +
           "AND r.startDate <= :endDate AND r.endDate >= :startDate")
    List<LeaveRequest> findDepartmentLeavesInDateRange(
            @Param("departmentId") Long departmentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") List<LeaveRequestStatus> statuses);

    @Query("SELECT r FROM LeaveRequest r " +
           "JOIN FETCH r.employee e " +
           "JOIN FETCH r.leaveType lt " +
           "LEFT JOIN FETCH e.department d " +
           "LEFT JOIN FETCH r.decidedBy db " +
           "WHERE r.staffingOverrideJustification IS NOT NULL " +
           "ORDER BY r.decisionTimestamp DESC")
    List<LeaveRequest> findStaffingOverridesWithDetails();

    @Query("SELECT r FROM LeaveRequest r " +
           "JOIN FETCH r.employee e " +
           "JOIN FETCH r.leaveType lt " +
           "LEFT JOIN FETCH e.department d " +
           "WHERE r.status = :status " +
           "AND r.startDate <= :endDate AND r.endDate >= :startDate " +
           "ORDER BY r.startDate ASC")
    List<LeaveRequest> findLeavesInDateRangeWithDetails(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") LeaveRequestStatus status);
}

