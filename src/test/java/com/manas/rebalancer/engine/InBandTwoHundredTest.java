package com.manas.rebalancer.engine;

import com.manas.rebalancer.fixtures.InBandAccountFixtures;
import com.manas.rebalancer.model.GoalAccountEntity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Claim 4: 0 proposals on 200 in-band accounts. */
class InBandTwoHundredTest {

    private final DriftBandRebalancingEngine engine = new DriftBandRebalancingEngine();

    @Test
    void twoHundredInBandAccountsProduceZeroProposals() {
        List<GoalAccountEntity> accounts = InBandAccountFixtures.generate(200);
        assertEquals(200, accounts.size());

        int proposalsRaised = 0;
        for (GoalAccountEntity account : accounts) {
            Optional<RebalanceProposalResult> result = engine.evaluate(account);
            if (result.isPresent()) {
                proposalsRaised++;
            }
        }
        System.out.println("InBandTwoHundredTest: " + proposalsRaised + " proposals raised on 200 in-band accounts");
        assertTrue(proposalsRaised == 0, "expected 0 proposals on in-band accounts, got " + proposalsRaised);
    }
}
