package com.manas.rebalancer;

import com.manas.rebalancer.model.GoalAccountEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Claim 1: the seeded population really is 5,000 accounts, not approximately. */
class RebalancerApplicationPopulationTest {

    @Test
    void fullPopulationIsExactlyFiveThousandAccounts() {
        List<GoalAccountEntity> population = RebalancerApplication.buildFullPopulation();
        assertEquals(5000, population.size());
        assertEquals(5000, population.stream().map(GoalAccountEntity::getAccountId).distinct().count(),
                "every account id must be unique");
    }
}
