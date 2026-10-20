package com.anudip.employee_leave.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_transactions",
       indexes = {
           @Index(name = "idx_tx_employee", columnList = "employee_id"),
           @Index(name = "idx_tx_balance", columnList = "leave_balance_id"),
           @Index(name = "idx_tx_idempotency", columnList = "idempotency_key", unique = true)
       })
public class LeaveTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_balance_id", nullable = false)
    private LeaveBalance leaveBalance;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(name = "leave_year", nullable = false)
    private int leaveYear;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 35)
    private LeaveTransactionType transactionType;

    @Column(name = "amount", precision = 6, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "previous_balance", precision = 6, scale = 2, nullable = false)
    private BigDecimal previousBalance;

    @Column(name = "resulting_balance", precision = 6, scale = 2, nullable = false)
    private BigDecimal resultingBalance;

    @Column(name = "reason", length = 500, nullable = false)
    private String reason;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "idempotency_key", length = 150, unique = true)
    private String idempotencyKey;

    @Column(name = "actor", length = 100, nullable = false)
    private String actor = "HR_ADMIN";

    @Column(name = "transaction_timestamp", nullable = false)
    private LocalDateTime transactionTimestamp = LocalDateTime.now();

    public LeaveTransaction() {
    }

    public LeaveTransaction(LeaveBalance leaveBalance, Employee employee, LeaveType leaveType, int leaveYear,
                            LeaveTransactionType transactionType, BigDecimal amount,
                            BigDecimal previousBalance, BigDecimal resultingBalance,
                            String reason, String referenceNumber, String idempotencyKey, String actor) {
        this.leaveBalance = leaveBalance;
        this.employee = employee;
        this.leaveType = leaveType;
        this.leaveYear = leaveYear;
        this.transactionType = transactionType;
        this.amount = amount != null ? amount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.previousBalance = previousBalance != null ? previousBalance.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.resultingBalance = resultingBalance != null ? resultingBalance.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.reason = reason;
        this.referenceNumber = referenceNumber;
        this.idempotencyKey = idempotencyKey;
        this.actor = actor != null ? actor : "HR_ADMIN";
        this.transactionTimestamp = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LeaveBalance getLeaveBalance() {
        return leaveBalance;
    }

    public void setLeaveBalance(LeaveBalance leaveBalance) {
        this.leaveBalance = leaveBalance;
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

    public int getLeaveYear() {
        return leaveYear;
    }

    public void setLeaveYear(int leaveYear) {
        this.leaveYear = leaveYear;
    }

    public LeaveTransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(LeaveTransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getPreviousBalance() {
        return previousBalance;
    }

    public void setPreviousBalance(BigDecimal previousBalance) {
        this.previousBalance = previousBalance;
    }

    public BigDecimal getResultingBalance() {
        return resultingBalance;
    }

    public void setResultingBalance(BigDecimal resultingBalance) {
        this.resultingBalance = resultingBalance;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public LocalDateTime getTransactionTimestamp() {
        return transactionTimestamp;
    }

    public void setTransactionTimestamp(LocalDateTime transactionTimestamp) {
        this.transactionTimestamp = transactionTimestamp;
    }
}

