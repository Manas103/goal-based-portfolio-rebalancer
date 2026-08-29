package com.manas.rebalancer.fixtures;

import com.manas.rebalancer.model.GoalAccountEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The remaining, unconstrained part of the 5,000-account synthetic
 * population (claim 1). Unlike {@link BreachScenarioFixtures} and
 * {@link InBandAccountFixtures}, these accounts are not individually
 * hand-verified; they exist to give the engine a realistic-shaped
 * population to run over and to give {@code ReferenceOracleDiffTest} (claim
 * 5) a meaningful sample of accounts that produce proposals, drawn from a
 * seeded, deterministic {@link Random} so the run is reproducible.
 */
public final class RandomAccountFixtures {

    private record Profile(String name, int equity, int bond, int cash) {
    }

    private static final List<Profile> PROFILES = List.of(
            new Profile("R-CONSERVATIVE", 30, 50, 20),
            new Profile("R-MOD-CONSERVATIVE", 40, 45, 15),
            new Profile("R-BALANCED", 55, 35, 10),
            new Profile("R-MOD-GROWTH", 65, 27, 8),
            new Profile("R-GROWTH", 80, 15, 5),
            new Profile("R-AGGRESSIVE", 90, 7, 3)
    );

    public static List<GoalAccountEntity> generate(int count, long seed) {
        Random random = new Random(seed);
        List<GoalAccountEntity> accounts = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            Profile profile = PROFILES.get(random.nextInt(PROFILES.size()));

            int actualEquityPct;
            int actualBondPct;
            int actualCashPct;
            int attempts = 0;
            do {
                int driftE = random.nextInt(21) - 10; // [-10, 10]
                int driftB = random.nextInt(21) - 10; // [-10, 10]
                actualEquityPct = profile.equity() + driftE;
                actualBondPct = profile.bond() + driftB;
                actualCashPct = 100 - actualEquityPct - actualBondPct;
                attempts++;
            } while ((actualEquityPct < 0 || actualBondPct < 0 || actualCashPct < 0 || actualCashPct > 60) && attempts < 50);

            int dollars = 10_000 + random.nextInt(740_000);
            int cents = random.nextInt(100);
            BigDecimal totalValue = BigDecimal.valueOf(dollars).setScale(2)
                    .add(BigDecimal.valueOf(cents).movePointLeft(2));

            BigDecimal equityValue = totalValue.multiply(BigDecimal.valueOf(actualEquityPct))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal bondValue = totalValue.multiply(BigDecimal.valueOf(actualBondPct))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal cashValue = totalValue.subtract(equityValue).subtract(bondValue);

            accounts.add(new GoalAccountEntity(
                    "RANDOM-" + String.format("%05d", i),
                    profile.name(),
                    BigDecimal.valueOf(profile.equity()), BigDecimal.valueOf(profile.bond()), BigDecimal.valueOf(profile.cash()),
                    equityValue, bondValue, cashValue));
        }
        return accounts;
    }

    private RandomAccountFixtures() {
    }
}
