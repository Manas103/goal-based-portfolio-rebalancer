package com.manas.rebalancer.engine;

import com.manas.rebalancer.PopulationFixtures;
import com.manas.rebalancer.model.GoalAccountEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Every proposed trade set must conserve money: buys and sells net to
 * exactly zero (no transaction costs are modeled, see the README), and
 * post-trade equity + bond + cash must equal the pre-trade total exactly.
 * Run over the full 5,000-account population, not just the 40 hand-checked
 * scenarios.
 */
class ConservationInvariantTest {

    private final DriftBandRebalancingEngine engine = new DriftBandRebalancingEngine();

    @Test
    void everyProposalConservesTotalValue() {
        List<GoalAccountEntity> population = PopulationFixtures.fullPopulation();
        int checked = 0;
        for (GoalAccountEntity account : population) {
            Optional<RebalanceProposalResult> result = engine.evaluate(account);
            if (result.isEmpty()) {
                continue;
            }
            RebalanceProposalResult r = result.get();
            BigDecimal tradeSum = r.equityTradeAmount().add(r.bondTradeAmount()).add(r.cashTradeAmount());
            assertEquals(0, tradeSum.compareTo(BigDecimal.ZERO), r.accountId() + " trades did not net to zero: " + tradeSum);

            BigDecimal postSum = r.postEquityValue().add(r.postBondValue()).add(r.postCashValue());
            assertEquals(0, postSum.compareTo(r.preTotalValue()), r.accountId() + " post-trade values do not sum to the pre-trade total");
            checked++;
        }
        System.out.println("ConservationInvariantTest: " + checked + " of " + population.size() + " accounts produced a proposal, all conserved");
    }
}
