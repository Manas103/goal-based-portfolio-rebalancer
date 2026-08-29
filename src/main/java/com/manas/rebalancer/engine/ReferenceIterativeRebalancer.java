package com.manas.rebalancer.engine;

import com.manas.rebalancer.model.AssetClass;
import com.manas.rebalancer.model.GoalAccountEntity;
import com.manas.rebalancer.model.TriggeringBand;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The independent reference oracle for claim 5 (BUILDER.md: "a reference
 * oracle where correctness is non-obvious"). Deliberately coded in a
 * different shape than {@link DriftBandRebalancingEngine}: a single loop
 * over {@link AssetClass#values()} driving three parallel arrays, rather
 * than {@link DriftBandRebalancingEngine}'s named per-field arithmetic, and
 * cash's target value is derived as the running total's remainder inside
 * that same loop rather than computed as a separate final subtraction. See
 * the README's "Findings" section: the first version of this class rounded
 * every asset class's target value independently (including cash) instead
 * of deriving cash as a remainder, which disagreed with the real engine by
 * a cent on a real, measured fraction of the population; this is the
 * version after that root cause was found and fixed.
 */
public final class ReferenceIterativeRebalancer {

    private static final int PCT_SCALE = 6;
    private static final AssetClass[] ORDER = {AssetClass.EQUITY, AssetClass.BOND, AssetClass.CASH};

    public Optional<RebalanceProposalResult> evaluate(GoalAccountEntity account) {
        BigDecimal total = account.totalValue();

        BigDecimal[] currentValue = {account.getEquityValue(), account.getBondValue(), account.getCashValue()};
        BigDecimal[] targetPct = {account.getTargetEquityPct(), account.getTargetBondPct(), account.getTargetCashPct()};
        BigDecimal[] currentPct = new BigDecimal[3];
        BigDecimal[] drift = new BigDecimal[3];
        boolean[] breached = new boolean[3];

        for (int i = 0; i < ORDER.length; i++) {
            currentPct[i] = currentValue[i].divide(total, PCT_SCALE, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            drift[i] = currentPct[i].subtract(targetPct[i]);
        }
        breached[0] = drift[0].abs().compareTo(RebalancePolicy.DRIFT_BAND_PCT) > 0;
        breached[1] = drift[1].abs().compareTo(RebalancePolicy.DRIFT_BAND_PCT) > 0;
        breached[2] = currentPct[2].compareTo(RebalancePolicy.CASH_MIN_PCT) < 0
                || currentPct[2].compareTo(RebalancePolicy.CASH_MAX_PCT) > 0;

        boolean anyBreach = false;
        List<TriggeringBand> bands = new ArrayList<>();
        for (int i = 0; i < ORDER.length; i++) {
            if (breached[i]) {
                anyBreach = true;
                bands.add(ORDER[i] == AssetClass.EQUITY ? TriggeringBand.EQUITY_DRIFT
                        : ORDER[i] == AssetClass.BOND ? TriggeringBand.BOND_DRIFT
                        : TriggeringBand.CASH_BAND);
            }
        }
        if (!anyBreach) {
            return Optional.empty();
        }

        // ATTEMPT 2 (see README "Findings"): attempt 1 rounded every asset
        // class's target value independently, cash included, which disagreed
        // with the real engine on 988 of 5,000 accounts by exactly one cent,
        // always on the cash leg. Cash's target value is now the running
        // total's remainder after equity and bond are rounded, the same
        // conservation rule DriftBandRebalancingEngine applies, just derived
        // inside this class's loop instead of as a final named subtraction.
        BigDecimal[] targetValue = new BigDecimal[3];
        BigDecimal roundedSoFar = BigDecimal.ZERO;
        for (int i = 0; i < ORDER.length; i++) {
            if (ORDER[i] == AssetClass.CASH) {
                targetValue[i] = total.subtract(roundedSoFar);
            } else {
                targetValue[i] = total.multiply(targetPct[i]).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                roundedSoFar = roundedSoFar.add(targetValue[i]);
            }
        }

        BigDecimal[] trade = new BigDecimal[3];
        BigDecimal[] postPct = new BigDecimal[3];
        for (int i = 0; i < ORDER.length; i++) {
            trade[i] = targetValue[i].subtract(currentValue[i]);
            postPct[i] = targetValue[i].divide(total, PCT_SCALE, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        }

        return Optional.of(new RebalanceProposalResult(
                account.getAccountId(),
                bands,
                total, currentValue[0], currentValue[1], currentValue[2],
                currentPct[0], currentPct[1], currentPct[2],
                total, targetValue[0], targetValue[1], targetValue[2],
                postPct[0], postPct[1], postPct[2],
                trade[0], trade[1], trade[2]));
    }
}
