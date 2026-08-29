package com.manas.rebalancer.engine;

import com.manas.rebalancer.PopulationFixtures;
import com.manas.rebalancer.model.GoalAccountEntity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Claim 5: exact agreement with an independent reference rebalancer, over
 * the full 5,000-account population. See {@link ReferenceIterativeRebalancer}'s
 * class Javadoc and the README's Findings section for the real disagreement
 * this test surfaced on its first honest run and how it was root-caused.
 */
class ReferenceOracleDiffTest {

    private final DriftBandRebalancingEngine engine = new DriftBandRebalancingEngine();
    private final ReferenceIterativeRebalancer reference = new ReferenceIterativeRebalancer();

    @Test
    void referenceRebalancerAgreesExactlyWithTheRealEngine() {
        List<GoalAccountEntity> population = PopulationFixtures.fullPopulation();
        int compared = 0;
        int disagreements = 0;
        StringBuilder detail = new StringBuilder();

        for (GoalAccountEntity account : population) {
            Optional<RebalanceProposalResult> real = engine.evaluate(account);
            Optional<RebalanceProposalResult> ref = reference.evaluate(account);
            compared++;

            if (real.isPresent() != ref.isPresent()) {
                disagreements++;
                detail.append(account.getAccountId()).append(": presence mismatch (real=").append(real.isPresent())
                        .append(", reference=").append(ref.isPresent()).append(")\n");
                continue;
            }
            if (real.isPresent()) {
                RebalanceProposalResult r = real.get();
                RebalanceProposalResult f = ref.get();
                boolean equal = r.equityTradeAmount().compareTo(f.equityTradeAmount()) == 0
                        && r.bondTradeAmount().compareTo(f.bondTradeAmount()) == 0
                        && r.cashTradeAmount().compareTo(f.cashTradeAmount()) == 0
                        && r.triggeringBands().equals(f.triggeringBands());
                if (!equal) {
                    disagreements++;
                    detail.append(account.getAccountId())
                            .append(": real=[eq=").append(r.equityTradeAmount()).append(",bo=").append(r.bondTradeAmount())
                            .append(",ca=").append(r.cashTradeAmount()).append("] reference=[eq=").append(f.equityTradeAmount())
                            .append(",bo=").append(f.bondTradeAmount()).append(",ca=").append(f.cashTradeAmount()).append("]\n");
                }
            }
        }

        System.out.println("ReferenceOracleDiffTest: compared " + compared + " accounts, " + disagreements + " disagreements");
        if (disagreements > 0) {
            System.out.println(detail.substring(0, Math.min(detail.length(), 2000)));
        }
        assertEquals(0, disagreements, disagreements + " of " + compared + " accounts disagreed with the reference rebalancer");
    }
}
