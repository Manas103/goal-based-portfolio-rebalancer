package com.manas.rebalancer.fixtures;

import com.manas.rebalancer.model.GoalAccountEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * The 200 seeded in-band accounts behind claim 4. Each account is
 * constructed, not sampled, to sit inside every band: equity and bond each
 * drift by a whole number of percentage points in {@code [-4, 4]} (strictly
 * inside the 5-point drift band), and the two drifts are opposite and equal
 * ({@code bondDrift = -equityDrift}), so cash's actual percentage equals its
 * target percentage exactly, and every profile's target cash percentage
 * (7, 8, 10 or 12) is comfortably inside the [2%, 15%] cash band. No account
 * this class produces can breach any band; {@code InBandTwoHundredTest}
 * asserts the engine proposes nothing for any of them.
 */
public final class InBandAccountFixtures {

    private record Profile(String name, int equity, int bond, int cash) {
    }

    private static final List<Profile> PROFILES = List.of(
            new Profile("P1", 60, 32, 8),
            new Profile("P2", 45, 45, 10),
            new Profile("P3", 70, 23, 7),
            new Profile("P4", 50, 40, 10),
            new Profile("P5", 35, 53, 12)
    );

    public static List<GoalAccountEntity> generate(int count) {
        List<GoalAccountEntity> accounts = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Profile profile = PROFILES.get(i % PROFILES.size());
            int drift = (i % 9) - 4; // cycles -4, -3, ..., 0, ..., 4
            int actualEquityPct = profile.equity() + drift;
            int actualBondPct = profile.bond() - drift;
            int actualCashPct = profile.cash();

            BigDecimal totalValue = BigDecimal.valueOf(50_000L + (i * 1_500L)).setScale(2, RoundingMode.UNNECESSARY);
            BigDecimal equityValue = totalValue.multiply(BigDecimal.valueOf(actualEquityPct))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal bondValue = totalValue.multiply(BigDecimal.valueOf(actualBondPct))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal cashValue = totalValue.subtract(equityValue).subtract(bondValue);

            accounts.add(new GoalAccountEntity(
                    "INBAND-" + String.format("%04d", i),
                    profile.name(),
                    BigDecimal.valueOf(profile.equity()), BigDecimal.valueOf(profile.bond()), BigDecimal.valueOf(profile.cash()),
                    equityValue, bondValue, cashValue));
        }
        return accounts;
    }

    private InBandAccountFixtures() {
    }
}
