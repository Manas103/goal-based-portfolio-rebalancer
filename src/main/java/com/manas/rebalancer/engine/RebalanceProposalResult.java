package com.manas.rebalancer.engine;

import com.manas.rebalancer.model.TriggeringBand;

import java.math.BigDecimal;
import java.util.List;

/**
 * In-memory result of evaluating one account: which band(s) fired, the full
 * pre-trade and post-trade allocation, and the proposed trade per asset
 * class. Produced by both {@link DriftBandRebalancingEngine} (the real
 * engine) and {@link ReferenceIterativeRebalancer} (the independent oracle),
 * so the two can be diffed field by field without either one knowing about
 * the ledger or JPA.
 */
public record RebalanceProposalResult(
        String accountId,
        List<TriggeringBand> triggeringBands,
        BigDecimal preTotalValue,
        BigDecimal preEquityValue,
        BigDecimal preBondValue,
        BigDecimal preCashValue,
        BigDecimal preEquityPct,
        BigDecimal preBondPct,
        BigDecimal preCashPct,
        BigDecimal postTotalValue,
        BigDecimal postEquityValue,
        BigDecimal postBondValue,
        BigDecimal postCashValue,
        BigDecimal postEquityPct,
        BigDecimal postBondPct,
        BigDecimal postCashPct,
        BigDecimal equityTradeAmount,
        BigDecimal bondTradeAmount,
        BigDecimal cashTradeAmount) {

    public String triggeringBandsAsString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < triggeringBands.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(triggeringBands.get(i).name());
        }
        return sb.toString();
    }
}
