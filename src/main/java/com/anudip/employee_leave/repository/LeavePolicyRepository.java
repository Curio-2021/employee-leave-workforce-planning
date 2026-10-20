package com.anudip.employee_leave.repository;

import com.anudip.employee_leave.entity.LeavePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeavePolicyRepository extends JpaRepository<LeavePolicy, Long> {

    boolean existsByPolicyNameIgnoreCaseAndEffectiveYear(String policyName, int effectiveYear);

    boolean existsByPolicyNameIgnoreCaseAndEffectiveYearAndIdNot(String policyName, int effectiveYear, Long id);

    @Query("SELECT p FROM LeavePolicy p " +
           "JOIN FETCH p.leaveType " +
           "LEFT JOIN FETCH p.applicableDepartment " +
           "ORDER BY p.effectiveYear DESC, p.policyName ASC")
    List<LeavePolicy> findAllWithDetails();

    List<LeavePolicy> findByLeaveTypeId(Long leaveTypeId);

    List<LeavePolicy> findByEffectiveYear(int effectiveYear);
}

