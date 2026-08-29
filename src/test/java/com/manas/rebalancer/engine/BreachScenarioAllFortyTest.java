package com.manas.rebalancer.engine;

import com.manas.rebalancer.fixtures.BreachCase;
import com.manas.rebalancer.fixtures.BreachScenarioFixtures;
import com.manas.rebalancer.model.GoalAccountEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Claim 3: 40 of 40 seeded drift and cash-band breaches produce the correct trade set. */
class BreachScenarioAllFortyTest {

    private final DriftBandRebalancingEngine engine = new DriftBandRebalancingEngine();

    @Test
    void allFortySeededBreachesProduceTheExpectedTradeSet() {
        assertEquals(40, BreachScenarioFixtures.ALL.size(), "fixture set itself must be exactly 40 cases");

        int correct = 0;
        for (BreachCase breachCase : BreachScenarioFixtures.ALL) {
            GoalAccountEntity account = breachCase.toAccount();
            Optional<RebalanceProposalResult> result = engine.evaluate(account);
            assertTrue(result.isPresent(), breachCase.id() + " should have triggered a proposal");
            RebalanceProposalResult r = result.get();

            assertEquals(1, r.triggeringBands().size(), breachCase.id() + " should fire exactly one band");
            assertEquals(breachCase.expectedBand(), r.triggeringBands().get(0), breachCase.id() + " wrong band");

            assertEquals(0, r.equityTradeAmount().compareTo(BigDecimal.valueOf(breachCase.expectedEquityTrade())),
                    breachCase.id() + " equity trade: expected " + breachCase.expectedEquityTrade() + " got " + r.equityTradeAmount());
            assertEquals(0, r.bondTradeAmount().compareTo(BigDecimal.valueOf(breachCase.expectedBondTrade())),
                    breachCase.id() + " bond trade: expected " + breachCase.expectedBondTrade() + " got " + r.bondTradeAmount());
            assertEquals(0, r.cashTradeAmount().compareTo(BigDecimal.valueOf(breachCase.expectedCashTrade())),
                    breachCase.id() + " cash trade: expected " + breachCase.expectedCashTrade() + " got " + r.cashTradeAmount());

            correct++;
        }
        System.out.println("BreachScenarioAllFortyTest: " + correct + " of " + BreachScenarioFixtures.ALL.size() + " seeded breaches produced the correct trade set");
        assertEquals(40, correct);
    }
}
