package com.manas.rebalancer.web;

import com.manas.rebalancer.model.RebalanceProposalEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;

/**
 * What the Angular console actually renders: every ledger field plus a
 * plain-English {@code explanation} built server side, so the console never
 * has to know the rebalancing rule itself, only how to display a sentence.
 */
public record ProposalResponse(
        Long id,
        String accountId,
        String triggeringBands,
        String ruleVersion,
        Instant proposedAt,
        BigDecimal preEquityPct, BigDecimal preBondPct, BigDecimal preCashPct,
        BigDecimal postEquityPct, BigDecimal postBondPct, BigDecimal postCashPct,
        BigDecimal equityTradeAmount, BigDecimal bondTradeAmount, BigDecimal cashTradeAmount,
        String explanation) {

    public static ProposalResponse from(RebalanceProposalEntity e) {
        return new ProposalResponse(
                e.getId(), e.getAccountId(), e.getTriggeringBands(), e.getRuleVersion(), e.getProposedAt(),
                e.getPreEquityPct(), e.getPreBondPct(), e.getPreCashPct(),
                e.getPostEquityPct(), e.getPostBondPct(), e.getPostCashPct(),
                e.getEquityTradeAmount(), e.getBondTradeAmount(), e.getCashTradeAmount(),
                explain(e));
    }

    private static String explain(RebalanceProposalEntity e) {
        String bands = e.getTriggeringBands().replace(",", " and ").replace("_", " ").toLowerCase(Locale.ROOT);
        return String.format(Locale.ROOT,
                "%s fired. Equity %.2f%%->%.2f%%, bond %.2f%%->%.2f%%, cash %.2f%%->%.2f%%. Proposed trade: equity %s, bond %s, cash %s (rule %s).",
                bands,
                e.getPreEquityPct(), e.getPostEquityPct(),
                e.getPreBondPct(), e.getPostBondPct(),
                e.getPreCashPct(), e.getPostCashPct(),
                money(e.getEquityTradeAmount()), money(e.getBondTradeAmount()), money(e.getCashTradeAmount()),
                e.getRuleVersion());
    }

    private static String money(BigDecimal v) {
        return (v.signum() >= 0 ? "+$" : "-$") + v.abs();
    }
}
