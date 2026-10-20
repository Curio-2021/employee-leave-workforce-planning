package com.anudip.employee_leave.repository;

import com.anudip.employee_leave.entity.LeaveTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveTransactionRepository extends JpaRepository<LeaveTransaction, Long> {

    List<LeaveTransaction> findByLeaveBalanceIdOrderByTransactionTimestampDesc(Long leaveBalanceId);

    List<LeaveTransaction> findByEmployeeIdOrderByTransactionTimestampDesc(Long employeeId);

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<LeaveTransaction> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT t FROM LeaveTransaction t " +
           "JOIN FETCH t.employee e " +
           "JOIN FETCH t.leaveType " +
           "JOIN FETCH t.leaveBalance " +
           "WHERE (:employeeId IS NULL OR e.id = :employeeId) " +
           "AND (:leaveTypeId IS NULL OR t.leaveType.id = :leaveTypeId) " +
           "AND (:leaveYear IS NULL OR t.leaveYear = :leaveYear) " +
           "ORDER BY t.transactionTimestamp DESC")
    List<LeaveTransaction> findFiltered(@Param("employeeId") Long employeeId,
                                        @Param("leaveTypeId") Long leaveTypeId,
                                        @Param("leaveYear") Integer leaveYear);

    @Query("SELECT t FROM LeaveTransaction t " +
           "JOIN FETCH t.employee e " +
           "JOIN FETCH t.leaveType lt " +
           "LEFT JOIN FETCH e.department d " +
           "WHERE t.transactionType IN :types " +
           "ORDER BY t.transactionTimestamp DESC")
    List<LeaveTransaction> findByTransactionTypeInWithDetails(@Param("types") List<com.anudip.employee_leave.entity.LeaveTransactionType> types);
}

