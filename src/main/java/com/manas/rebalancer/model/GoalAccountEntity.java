package com.manas.rebalancer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * One goal-based advisory account: a target allocation across Equity, Bond
 * and Cash, and the account's current (actual) dollar holding in each of
 * those three asset classes. {@code equityValue + bondValue + cashValue}
 * always equals {@code totalValue} by construction; nothing in this
 * repository ever leaves that identity out of balance (see
 * {@code ConservationInvariantTest}).
 *
 * <p>Target percentages are policy (chosen once when the goal is set up),
 * not measured; drift and cash-band thresholds are a fixed,
 * institution-wide policy applied to every account (see
 * {@code RebalancePolicy}), not a per-account field, which is a deliberate
 * simplification stated in the README.
 */
@Entity
@Table(name = "goal_account")
public class GoalAccountEntity {

    @Id
    @Column(name = "account_id", nullable = false, updatable = false)
    private String accountId;

    @Column(name = "profile_name", nullable = false)
    private String profileName;

    @Column(name = "target_equity_pct", nullable = false, precision = 7, scale = 4)
    private BigDecimal targetEquityPct;

    @Column(name = "target_bond_pct", nullable = false, precision = 7, scale = 4)
    private BigDecimal targetBondPct;

    @Column(name = "target_cash_pct", nullable = false, precision = 7, scale = 4)
    private BigDecimal targetCashPct;

    @Column(name = "equity_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal equityValue;

    @Column(name = "bond_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal bondValue;

    @Column(name = "cash_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal cashValue;

    protected GoalAccountEntity() {
        // JPA
    }

    public GoalAccountEntity(String accountId, String profileName,
                              BigDecimal targetEquityPct, BigDecimal targetBondPct, BigDecimal targetCashPct,
                              BigDecimal equityValue, BigDecimal bondValue, BigDecimal cashValue) {
        this.accountId = accountId;
        this.profileName = profileName;
        this.targetEquityPct = targetEquityPct;
        this.targetBondPct = targetBondPct;
        this.targetCashPct = targetCashPct;
        this.equityValue = equityValue;
        this.bondValue = bondValue;
        this.cashValue = cashValue;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getProfileName() {
        return profileName;
    }

    public BigDecimal getTargetEquityPct() {
        return targetEquityPct;
    }

    public BigDecimal getTargetBondPct() {
        return targetBondPct;
    }

    public BigDecimal getTargetCashPct() {
        return targetCashPct;
    }

    public BigDecimal getEquityValue() {
        return equityValue;
    }

    public BigDecimal getBondValue() {
        return bondValue;
    }

    public BigDecimal getCashValue() {
        return cashValue;
    }

    public BigDecimal totalValue() {
        return equityValue.add(bondValue).add(cashValue);
    }
}
