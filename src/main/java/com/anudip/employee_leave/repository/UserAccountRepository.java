package com.anudip.employee_leave.repository;

import com.anudip.employee_leave.entity.Role;
import com.anudip.employee_leave.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    @Query("SELECT u FROM UserAccount u LEFT JOIN FETCH u.employee e LEFT JOIN FETCH e.department WHERE LOWER(u.username) = LOWER(:username)")
    Optional<UserAccount> findByUsernameWithDetails(@Param("username") String username);

    Optional<UserAccount> findByUsername(String username);

    Optional<UserAccount> findByEmployeeId(Long employeeId);

    Optional<UserAccount> findByActivationToken(String activationToken);

    Optional<UserAccount> findByPasswordResetToken(String passwordResetToken);

    boolean existsByUsername(String username);

    long countByRole(Role role);
}
