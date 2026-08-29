package com.manas.rebalancer.fixtures;

import com.manas.rebalancer.model.GoalAccountEntity;
import com.manas.rebalancer.model.TriggeringBand;

import java.math.BigDecimal;

/**
 * One of the 40 seeded, hand-computable drift and cash-band breach
 * scenarios (claim 3). Every scenario uses a $100,000 total value so that
 * one percentage point equals exactly $1,000 and every expected trade
 * amount below can be checked by hand: {@code expectedXTrade == (targetXPct
 * - actualXPct) * 1000}. Exactly one band is constructed to fire per
 * scenario (see {@code BreachScenarioFixtures}'s class Javadoc for how each
 * group is built), so {@code expectedBand} is a single value, not a list.
 */
public record BreachCase(
        String id,
        String profileName,
        int targetEquityPct,
        int targetBondPct,
        int targetCashPct,
        int actualEquityPct,
        int actualBondPct,
        int actualCashPct,
        int expectedEquityTrade,
        int expectedBondTrade,
        int expectedCashTrade,
        TriggeringBand expectedBand) {

    public static final BigDecimal TOTAL_VALUE = new BigDecimal("100000.00");

    public GoalAccountEntity toAccount() {
        BigDecimal equityValue = TOTAL_VALUE.multiply(BigDecimal.valueOf(actualEquityPct)).divide(BigDecimal.valueOf(100));
        BigDecimal bondValue = TOTAL_VALUE.multiply(BigDecimal.valueOf(actualBondPct)).divide(BigDecimal.valueOf(100));
        BigDecimal cashValue = TOTAL_VALUE.subtract(equityValue).subtract(bondValue);
        return new GoalAccountEntity(
                "BREACH-" + id,
                profileName,
                BigDecimal.valueOf(targetEquityPct), BigDecimal.valueOf(targetBondPct), BigDecimal.valueOf(targetCashPct),
                equityValue, bondValue, cashValue);
    }
}
