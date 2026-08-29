package com.manas.rebalancer.fixtures;

import com.manas.rebalancer.model.TriggeringBand;

import java.util.List;

import static com.manas.rebalancer.model.TriggeringBand.BOND_DRIFT;
import static com.manas.rebalancer.model.TriggeringBand.CASH_BAND;
import static com.manas.rebalancer.model.TriggeringBand.EQUITY_DRIFT;

/**
 * The 40 seeded breach scenarios behind claim 3. Every row is hand-checkable
 * against {@code RebalancePolicy} (drift band 5 percentage points, cash band
 * [2%, 15%]) and against {@code BreachCase}'s $1,000-per-percentage-point
 * arithmetic:
 *
 * <ul>
 *   <li><b>EQ1..EQ15</b>: equity drifts by more than 5 points off its target
 *   (breach), bond stays exactly at its target (0 drift, in band), cash
 *   absorbs the opposite of the equity move and is checked to stay inside
 *   [2%, 15%] (in band), so only {@code EQUITY_DRIFT} fires.</li>
 *   <li><b>BO1..BO15</b>: the mirror image, bond breaches, equity stays at
 *   target, cash absorbs the move and stays in band, so only
 *   {@code BOND_DRIFT} fires.</li>
 *   <li><b>CA1..CA10</b>: cash is pushed outside [2%, 15%] by a 4-point move
 *   that is absorbed entirely by equity (a 4-point drift is inside the
 *   5-point equity band, so equity does not also breach); bond is untouched.
 *   Only {@code CASH_BAND} fires.</li>
 * </ul>
 *
 * <p>Every row's three actual percentages sum to 100, every row's three
 * target percentages sum to 100, and every row's three expected trade
 * amounts sum to exactly 0 (rebalance-to-target moves money between asset
 * classes, it does not create or destroy it). {@code BreachScenarioAllFortyTest}
 * asserts all three of those identities in addition to diffing the engine's
 * actual output against the {@code expectedXTrade} and {@code expectedBand}
 * columns below.
 */
public final class BreachScenarioFixtures {

    public static final List<BreachCase> ALL = List.of(
            // --- EQ1..EQ15: equity-drift-only breaches -------------------------------------------------
            new BreachCase("EQ1", "P1", 60, 32, 8, 66, 32, 2, -6000, 0, 6000, EQUITY_DRIFT),
            new BreachCase("EQ2", "P1", 60, 32, 8, 54, 32, 14, 6000, 0, -6000, EQUITY_DRIFT),
            new BreachCase("EQ3", "P1", 60, 32, 8, 53, 32, 15, 7000, 0, -7000, EQUITY_DRIFT),
            new BreachCase("EQ4", "P2", 45, 45, 10, 51, 45, 4, -6000, 0, 6000, EQUITY_DRIFT),
            new BreachCase("EQ5", "P2", 45, 45, 10, 52, 45, 3, -7000, 0, 7000, EQUITY_DRIFT),
            new BreachCase("EQ6", "P2", 45, 45, 10, 53, 45, 2, -8000, 0, 8000, EQUITY_DRIFT),
            new BreachCase("EQ7", "P3", 70, 23, 7, 64, 23, 13, 6000, 0, -6000, EQUITY_DRIFT),
            new BreachCase("EQ8", "P3", 70, 23, 7, 63, 23, 14, 7000, 0, -7000, EQUITY_DRIFT),
            new BreachCase("EQ9", "P3", 70, 23, 7, 62, 23, 15, 8000, 0, -8000, EQUITY_DRIFT),
            new BreachCase("EQ10", "P4", 50, 40, 10, 56, 40, 4, -6000, 0, 6000, EQUITY_DRIFT),
            new BreachCase("EQ11", "P4", 50, 40, 10, 57, 40, 3, -7000, 0, 7000, EQUITY_DRIFT),
            new BreachCase("EQ12", "P4", 50, 40, 10, 58, 40, 2, -8000, 0, 8000, EQUITY_DRIFT),
            new BreachCase("EQ13", "P5", 35, 53, 12, 41, 53, 6, -6000, 0, 6000, EQUITY_DRIFT),
            new BreachCase("EQ14", "P5", 35, 53, 12, 42, 53, 5, -7000, 0, 7000, EQUITY_DRIFT),
            new BreachCase("EQ15", "P5", 35, 53, 12, 43, 53, 4, -8000, 0, 8000, EQUITY_DRIFT),

            // --- BO1..BO15: bond-drift-only breaches ----------------------------------------------------
            new BreachCase("BO1", "P1", 60, 32, 8, 60, 38, 2, 0, -6000, 6000, BOND_DRIFT),
            new BreachCase("BO2", "P1", 60, 32, 8, 60, 26, 14, 0, 6000, -6000, BOND_DRIFT),
            new BreachCase("BO3", "P1", 60, 32, 8, 60, 25, 15, 0, 7000, -7000, BOND_DRIFT),
            new BreachCase("BO4", "P2", 45, 45, 10, 45, 51, 4, 0, -6000, 6000, BOND_DRIFT),
            new BreachCase("BO5", "P2", 45, 45, 10, 45, 52, 3, 0, -7000, 7000, BOND_DRIFT),
            new BreachCase("BO6", "P2", 45, 45, 10, 45, 53, 2, 0, -8000, 8000, BOND_DRIFT),
            new BreachCase("BO7", "P3", 70, 23, 7, 70, 17, 13, 0, 6000, -6000, BOND_DRIFT),
            new BreachCase("BO8", "P3", 70, 23, 7, 70, 16, 14, 0, 7000, -7000, BOND_DRIFT),
            new BreachCase("BO9", "P3", 70, 23, 7, 70, 15, 15, 0, 8000, -8000, BOND_DRIFT),
            new BreachCase("BO10", "P4", 50, 40, 10, 50, 46, 4, 0, -6000, 6000, BOND_DRIFT),
            new BreachCase("BO11", "P4", 50, 40, 10, 50, 47, 3, 0, -7000, 7000, BOND_DRIFT),
            new BreachCase("BO12", "P4", 50, 40, 10, 50, 48, 2, 0, -8000, 8000, BOND_DRIFT),
            new BreachCase("BO13", "P5", 35, 53, 12, 35, 59, 6, 0, -6000, 6000, BOND_DRIFT),
            new BreachCase("BO14", "P5", 35, 53, 12, 35, 60, 5, 0, -7000, 7000, BOND_DRIFT),
            new BreachCase("BO15", "P5", 35, 53, 12, 35, 61, 4, 0, -8000, 8000, BOND_DRIFT),

            // --- CA1..CA10: cash-band-only breaches -----------------------------------------------------
            new BreachCase("CA1", "C1", 55, 40, 5, 59, 40, 1, -4000, 0, 4000, CASH_BAND),
            new BreachCase("CA2", "C2", 50, 45, 5, 54, 45, 1, -4000, 0, 4000, CASH_BAND),
            new BreachCase("CA3", "C3", 60, 35, 5, 64, 35, 1, -4000, 0, 4000, CASH_BAND),
            new BreachCase("CA4", "C4", 45, 51, 4, 49, 51, 0, -4000, 0, 4000, CASH_BAND),
            new BreachCase("CA5", "C5", 58, 38, 4, 62, 38, 0, -4000, 0, 4000, CASH_BAND),
            new BreachCase("CA6", "C6", 40, 48, 12, 36, 48, 16, 4000, 0, -4000, CASH_BAND),
            new BreachCase("CA7", "C7", 50, 37, 13, 46, 37, 17, 4000, 0, -4000, CASH_BAND),
            new BreachCase("CA8", "C8", 55, 31, 14, 51, 31, 18, 4000, 0, -4000, CASH_BAND),
            new BreachCase("CA9", "C9", 35, 51, 14, 31, 51, 18, 4000, 0, -4000, CASH_BAND),
            new BreachCase("CA10", "C10", 62, 25, 13, 58, 25, 17, 4000, 0, -4000, CASH_BAND)
    );

    private BreachScenarioFixtures() {
    }
}
