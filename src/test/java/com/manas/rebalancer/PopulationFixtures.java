package com.manas.rebalancer;

import com.manas.rebalancer.fixtures.BreachCase;
import com.manas.rebalancer.fixtures.BreachScenarioFixtures;
import com.manas.rebalancer.fixtures.InBandAccountFixtures;
import com.manas.rebalancer.fixtures.RandomAccountFixtures;
import com.manas.rebalancer.model.GoalAccountEntity;

import java.util.ArrayList;
import java.util.List;

/** Shared test helper building the exact same 5,000-account population {@code RebalancerApplication} seeds. */
public final class PopulationFixtures {

    public static final int TOTAL = 5000;

    public static List<GoalAccountEntity> fullPopulation() {
        List<GoalAccountEntity> population = new ArrayList<>(TOTAL);
        for (BreachCase breachCase : BreachScenarioFixtures.ALL) {
            population.add(breachCase.toAccount());
        }
        population.addAll(InBandAccountFixtures.generate(200));
        population.addAll(RandomAccountFixtures.generate(TOTAL - population.size(), 42L));
        return population;
    }

    private PopulationFixtures() {
    }
}
