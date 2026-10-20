package com.anudip.employee_leave.repository;

import com.anudip.employee_leave.entity.Employee;
import com.anudip.employee_leave.entity.EmploymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByEmployeeIdIgnoreCase(String employeeId);

    boolean existsByEmployeeIdIgnoreCaseAndIdNot(String employeeId, Long id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    long countByDepartmentId(Long departmentId);

    long countByEmploymentStatus(EmploymentStatus status);

    @Query("SELECT e FROM Employee e " +
           "LEFT JOIN FETCH e.department " +
           "LEFT JOIN FETCH e.reportingManager " +
           "ORDER BY e.fullName ASC")
    List<Employee> findAllWithDetails();

    @Query("SELECT e FROM Employee e " +
           "LEFT JOIN FETCH e.department " +
           "LEFT JOIN FETCH e.reportingManager " +
           "WHERE (:departmentId IS NULL OR e.department.id = :departmentId) " +
           "AND (:status IS NULL OR e.employmentStatus = :status) " +
           "ORDER BY e.fullName ASC")
    List<Employee> findFiltered(@Param("departmentId") Long departmentId,
                                @Param("status") EmploymentStatus status);

    @Query("SELECT e FROM Employee e " +
           "WHERE e.employmentStatus = 'ACTIVE' " +
           "AND (:excludeId IS NULL OR e.id != :excludeId) " +
           "ORDER BY e.fullName ASC")
    List<Employee> findEligibleManagers(@Param("excludeId") Long excludeId);

    @Query("SELECT e FROM Employee e " +
           "WHERE e.employmentStatus = 'ACTIVE' " +
           "AND (LOWER(e.department.name) LIKE '%hr%' OR LOWER(e.department.name) LIKE '%human resource%') " +
           "ORDER BY e.id ASC")
    List<Employee> findActiveHrEmployees();
}

