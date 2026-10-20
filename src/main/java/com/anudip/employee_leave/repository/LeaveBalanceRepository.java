package com.anudip.employee_leave.repository;

import com.anudip.employee_leave.entity.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    Optional<LeaveBalance> findByEmployeeIdAndLeaveTypeIdAndLeaveYear(Long employeeId, Long leaveTypeId, int leaveYear);

    boolean existsByEmployeeIdAndLeaveTypeIdAndLeaveYear(Long employeeId, Long leaveTypeId, int leaveYear);

    List<LeaveBalance> findByEmployeeIdAndLeaveYear(Long employeeId, int leaveYear);

    List<LeaveBalance> findByEmployeeId(Long employeeId);

    @Query("SELECT b FROM LeaveBalance b " +
           "JOIN FETCH b.employee e " +
           "JOIN FETCH e.department " +
           "JOIN FETCH b.leaveType " +
           "LEFT JOIN FETCH b.applicablePolicy " +
           "WHERE (:employeeId IS NULL OR e.id = :employeeId) " +
           "AND (:departmentId IS NULL OR e.department.id = :departmentId) " +
           "AND (:leaveTypeId IS NULL OR b.leaveType.id = :leaveTypeId) " +
           "AND (:leaveYear IS NULL OR b.leaveYear = :leaveYear) " +
           "ORDER BY e.fullName ASC, b.leaveType.name ASC")
    List<LeaveBalance> findFiltered(@Param("employeeId") Long employeeId,
                                    @Param("departmentId") Long departmentId,
                                    @Param("leaveTypeId") Long leaveTypeId,
                                    @Param("leaveYear") Integer leaveYear);

    @Query("SELECT b FROM LeaveBalance b " +
           "JOIN FETCH b.employee e " +
           "JOIN FETCH e.department " +
           "JOIN FETCH b.leaveType " +
           "LEFT JOIN FETCH b.applicablePolicy " +
           "WHERE b.id = :id")
    Optional<LeaveBalance> findByIdWithDetails(@Param("id") Long id);
}

