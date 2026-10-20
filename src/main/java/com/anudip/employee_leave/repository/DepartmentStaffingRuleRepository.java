package com.anudip.employee_leave.repository;

import com.anudip.employee_leave.entity.DepartmentStaffingRule;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentStaffingRuleRepository extends JpaRepository<DepartmentStaffingRule, Long> {

    @Query("SELECT r FROM DepartmentStaffingRule r " +
           "JOIN FETCH r.department d " +
           "WHERE d.id = :departmentId")
    Optional<DepartmentStaffingRule> findByDepartmentId(@Param("departmentId") Long departmentId);

    boolean existsByDepartmentId(Long departmentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM DepartmentStaffingRule r WHERE r.department.id = :departmentId")
    Optional<DepartmentStaffingRule> findByDepartmentIdWithLock(@Param("departmentId") Long departmentId);

    @Query("SELECT r FROM DepartmentStaffingRule r " +
           "JOIN FETCH r.department d " +
           "ORDER BY d.name ASC")
    List<DepartmentStaffingRule> findAllWithDepartment();
}

