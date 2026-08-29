package com.manas.rebalancer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One row of the append-only disclosure ledger: a single proposed trade set
 * for a single account, with the band(s) that triggered it, the account's
 * full pre-trade and post-trade allocation (every asset class plus cash, as
 * both dollars and percentages), the proposed trade amount per asset class,
 * and the rule version that produced it.
 *
 * <p>There is deliberately no setter on this class once constructed, and no
 * method anywhere in this repository that mutates a row after it is
 * written. {@code RebalanceProposalRepository} (see its Javadoc) extends
 * Spring Data's bare {@code Repository} marker interface rather than
 * {@code JpaRepository} and declares only {@code save}, {@code findAll},
 * {@code findByAccountIdOrderByProposedAtAsc} and {@code count}; there is no
 * {@code update}, {@code delete} or {@code deleteById} method anywhere in
 * this class's application-visible repository contract.
 * {@code LedgerAppendOnlyTest} asserts both properties by reflection and by
 * writing two proposals for the same account and confirming both persist as
 * two distinct rows.
 */
@Entity
@Table(name = "rebalance_proposal")
public class RebalanceProposalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    /** Comma-separated {@link TriggeringBand} names, e.g. "EQUITY_DRIFT" or "EQUITY_DRIFT,CASH_BAND". */
    @Column(name = "triggering_bands", nullable = false)
    private String triggeringBands;

    @Column(name = "rule_version", nullable = false)
    private String ruleVersion;

    @Column(name = "proposed_at", nullable = false)
    private Instant proposedAt;

    @Column(name = "pre_total_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal preTotalValue;
    @Column(name = "pre_equity_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal preEquityValue;
    @Column(name = "pre_bond_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal preBondValue;
    @Column(name = "pre_cash_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal preCashValue;
    @Column(name = "pre_equity_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal preEquityPct;
    @Column(name = "pre_bond_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal preBondPct;
    @Column(name = "pre_cash_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal preCashPct;

    @Column(name = "post_total_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal postTotalValue;
    @Column(name = "post_equity_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal postEquityValue;
    @Column(name = "post_bond_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal postBondValue;
    @Column(name = "post_cash_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal postCashValue;
    @Column(name = "post_equity_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal postEquityPct;
    @Column(name = "post_bond_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal postBondPct;
    @Column(name = "post_cash_pct", nullable = false, precision = 12, scale = 6)
    private BigDecimal postCashPct;

    @Column(name = "equity_trade_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal equityTradeAmount;
    @Column(name = "bond_trade_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal bondTradeAmount;
    @Column(name = "cash_trade_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal cashTradeAmount;

    protected RebalanceProposalEntity() {
        // JPA
    }

    public RebalanceProposalEntity(String accountId, String triggeringBands, String ruleVersion, Instant proposedAt,
                                    BigDecimal preTotalValue, BigDecimal preEquityValue, BigDecimal preBondValue, BigDecimal preCashValue,
                                    BigDecimal preEquityPct, BigDecimal preBondPct, BigDecimal preCashPct,
                                    BigDecimal postTotalValue, BigDecimal postEquityValue, BigDecimal postBondValue, BigDecimal postCashValue,
                                    BigDecimal postEquityPct, BigDecimal postBondPct, BigDecimal postCashPct,
                                    BigDecimal equityTradeAmount, BigDecimal bondTradeAmount, BigDecimal cashTradeAmount) {
        this.accountId = accountId;
        this.triggeringBands = triggeringBands;
        this.ruleVersion = ruleVersion;
        this.proposedAt = proposedAt;
        this.preTotalValue = preTotalValue;
        this.preEquityValue = preEquityValue;
        this.preBondValue = preBondValue;
        this.preCashValue = preCashValue;
        this.preEquityPct = preEquityPct;
        this.preBondPct = preBondPct;
        this.preCashPct = preCashPct;
        this.postTotalValue = postTotalValue;
        this.postEquityValue = postEquityValue;
        this.postBondValue = postBondValue;
        this.postCashValue = postCashValue;
        this.postEquityPct = postEquityPct;
        this.postBondPct = postBondPct;
        this.postCashPct = postCashPct;
        this.equityTradeAmount = equityTradeAmount;
        this.bondTradeAmount = bondTradeAmount;
        this.cashTradeAmount = cashTradeAmount;
    }

    public Long getId() {
        return id;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getTriggeringBands() {
        return triggeringBands;
    }

    public String getRuleVersion() {
        return ruleVersion;
    }

    public Instant getProposedAt() {
        return proposedAt;
    }

    public BigDecimal getPreTotalValue() {
        return preTotalValue;
    }

    public BigDecimal getPreEquityValue() {
        return preEquityValue;
    }

    public BigDecimal getPreBondValue() {
        return preBondValue;
    }

    public BigDecimal getPreCashValue() {
        return preCashValue;
    }

    public BigDecimal getPreEquityPct() {
        return preEquityPct;
    }

    public BigDecimal getPreBondPct() {
        return preBondPct;
    }

    public BigDecimal getPreCashPct() {
        return preCashPct;
    }

    public BigDecimal getPostTotalValue() {
        return postTotalValue;
    }

    public BigDecimal getPostEquityValue() {
        return postEquityValue;
    }

    public BigDecimal getPostBondValue() {
        return postBondValue;
    }

    public BigDecimal getPostCashValue() {
        return postCashValue;
    }

    public BigDecimal getPostEquityPct() {
        return postEquityPct;
    }

    public BigDecimal getPostBondPct() {
        return postBondPct;
    }

    public BigDecimal getPostCashPct() {
        return postCashPct;
    }

    public BigDecimal getEquityTradeAmount() {
        return equityTradeAmount;
    }

    public BigDecimal getBondTradeAmount() {
        return bondTradeAmount;
    }

    public BigDecimal getCashTradeAmount() {
        return cashTradeAmount;
    }
}
