package com.anudip.employee_leave.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "leave_balances",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_emp_leavetype_year", columnNames = {"employee_id", "leave_type_id", "leave_year"})
       })
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicable_policy_id")
    private LeavePolicy applicablePolicy;

    @Column(name = "leave_year", nullable = false)
    private int leaveYear;

    @Column(name = "carried_forward", precision = 6, scale = 2, nullable = false)
    private BigDecimal carriedForward = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "granted_entitlement", precision = 6, scale = 2, nullable = false)
    private BigDecimal grantedEntitlement = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "adjustment_credits", precision = 6, scale = 2, nullable = false)
    private BigDecimal adjustmentCredits = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "adjustment_debits", precision = 6, scale = 2, nullable = false)
    private BigDecimal adjustmentDebits = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "used_leave", precision = 6, scale = 2, nullable = false)
    private BigDecimal usedLeave = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "available_balance", precision = 6, scale = 2, nullable = false)
    private BigDecimal availableBalance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Version
    private Long version;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated = LocalDateTime.now();

    // Do NOT cascade remove transactions to guarantee append-only ledger integrity
    @OneToMany(mappedBy = "leaveBalance")
    private List<LeaveTransaction> transactions = new ArrayList<>();

    public LeaveBalance() {
    }

    public LeaveBalance(Employee employee, LeaveType leaveType, LeavePolicy applicablePolicy, int leaveYear) {
        this.employee = employee;
        this.leaveType = leaveType;
        this.applicablePolicy = applicablePolicy;
        this.leaveYear = leaveYear;
        recalculateBalance();
    }

    public void recalculateBalance() {
        BigDecimal totalCredits = safeAmount(this.carriedForward)
                .add(safeAmount(this.grantedEntitlement))
                .add(safeAmount(this.adjustmentCredits));

        BigDecimal totalDebits = safeAmount(this.adjustmentDebits)
                .add(safeAmount(this.usedLeave));

        this.availableBalance = totalCredits.subtract(totalDebits).setScale(2, RoundingMode.HALF_UP);
        this.lastUpdated = LocalDateTime.now();
    }

    private BigDecimal safeAmount(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
    }

    public LeavePolicy getApplicablePolicy() {
        return applicablePolicy;
    }

    public void setApplicablePolicy(LeavePolicy applicablePolicy) {
        this.applicablePolicy = applicablePolicy;
    }

    public int getLeaveYear() {
        return leaveYear;
    }

    public void setLeaveYear(int leaveYear) {
        this.leaveYear = leaveYear;
    }

    public BigDecimal getCarriedForward() {
        return carriedForward;
    }

    public void setCarriedForward(BigDecimal carriedForward) {
        this.carriedForward = safeAmount(carriedForward);
        recalculateBalance();
    }

    public BigDecimal getGrantedEntitlement() {
        return grantedEntitlement;
    }

    public void setGrantedEntitlement(BigDecimal grantedEntitlement) {
        this.grantedEntitlement = safeAmount(grantedEntitlement);
        recalculateBalance();
    }

    public BigDecimal getAdjustmentCredits() {
        return adjustmentCredits;
    }

    public void setAdjustmentCredits(BigDecimal adjustmentCredits) {
        this.adjustmentCredits = safeAmount(adjustmentCredits);
        recalculateBalance();
    }

    public BigDecimal getAdjustmentDebits() {
        return adjustmentDebits;
    }

    public void setAdjustmentDebits(BigDecimal adjustmentDebits) {
        this.adjustmentDebits = safeAmount(adjustmentDebits);
        recalculateBalance();
    }

    public BigDecimal getUsedLeave() {
        return usedLeave;
    }

    public void setUsedLeave(BigDecimal usedLeave) {
        this.usedLeave = safeAmount(usedLeave);
        recalculateBalance();
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public List<LeaveTransaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<LeaveTransaction> transactions) {
        this.transactions = transactions;
    }
}

