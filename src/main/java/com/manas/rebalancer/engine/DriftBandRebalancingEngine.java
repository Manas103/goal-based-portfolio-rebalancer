package com.manas.rebalancer.engine;

import com.manas.rebalancer.model.GoalAccountEntity;
import com.manas.rebalancer.model.TriggeringBand;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The real engine. For each account: compute current allocation percentages,
 * check the two drift bands and the cash band, and if any fired, propose a
 * trade set that rebalances the account all the way back to its target
 * allocation.
 *
 * <p><b>Why rebalance-to-target, not rebalance-to-band-edge.</b> A
 * goal-based account's target weight is the actual goal-consistent
 * allocation; the band only decides *when* to act, not what the right
 * allocation is once acting. Stopping at the nearer band edge would leave
 * the account still drifting away from the one allocation that is actually
 * consistent with its goal, and would in the median case put it back into
 * breach again on the very next drift check, which would mean shipping
 * another rebalance run to cover the same account almost immediately. This
 * is stated as a design choice, not left implicit.
 *
 * <p><b>Why cash absorbs the rounding remainder.</b> Equity and bond target
 * dollar values are computed directly from the target percentage and
 * rounded to the cent (HALF_UP); cash's target dollar value is then
 * {@code totalValue - targetEquityValue - targetBondValue}, not an
 * independently rounded {@code totalValue * targetCashPct / 100}. This
 * guarantees the three post-trade values sum to exactly the pre-trade total
 * (see {@code ConservationInvariantTest}) with no floating remainder left
 * over. Cash is the natural bucket to absorb it: equity and bond are the two
 * classes actually traded to rebalance, and a sub-cent rounding remainder
 * landing in cash changes nothing about which trades are proposed.
 *
 * <p>No transaction costs are modeled. A proposed trade set is exactly
 * {@code target value - current value} per asset class; there is no
 * bid-ask spread, commission or slippage subtracted from either side, which
 * is why buys and sells always net to exactly zero before any such cost
 * (see {@code ConservationInvariantTest}) and would not, in a system that
 * modeled costs, after one.
 */
@Component
public final class DriftBandRebalancingEngine {

    private static final int PCT_SCALE = 6;

    public Optional<RebalanceProposalResult> evaluate(GoalAccountEntity account) {
        BigDecimal total = account.totalValue();

        BigDecimal currentEquityPct = percentOf(account.getEquityValue(), total);
        BigDecimal currentBondPct = percentOf(account.getBondValue(), total);
        BigDecimal currentCashPct = percentOf(account.getCashValue(), total);

        BigDecimal equityDrift = currentEquityPct.subtract(account.getTargetEquityPct());
        BigDecimal bondDrift = currentBondPct.subtract(account.getTargetBondPct());

        boolean equityBreach = equityDrift.abs().compareTo(RebalancePolicy.DRIFT_BAND_PCT) > 0;
        boolean bondBreach = bondDrift.abs().compareTo(RebalancePolicy.DRIFT_BAND_PCT) > 0;
        boolean cashBreach = currentCashPct.compareTo(RebalancePolicy.CASH_MIN_PCT) < 0
                || currentCashPct.compareTo(RebalancePolicy.CASH_MAX_PCT) > 0;

        if (!equityBreach && !bondBreach && !cashBreach) {
            return Optional.empty();
        }

        List<TriggeringBand> bands = new ArrayList<>();
        if (equityBreach) {
            bands.add(TriggeringBand.EQUITY_DRIFT);
        }
        if (bondBreach) {
            bands.add(TriggeringBand.BOND_DRIFT);
        }
        if (cashBreach) {
            bands.add(TriggeringBand.CASH_BAND);
        }

        BigDecimal targetEquityValue = total.multiply(account.getTargetEquityPct())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal targetBondValue = total.multiply(account.getTargetBondPct())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        // Residual: guarantees targetEquityValue + targetBondValue + targetCashValue == total exactly.
        BigDecimal targetCashValue = total.subtract(targetEquityValue).subtract(targetBondValue);

        BigDecimal equityTrade = targetEquityValue.subtract(account.getEquityValue());
        BigDecimal bondTrade = targetBondValue.subtract(account.getBondValue());
        BigDecimal cashTrade = targetCashValue.subtract(account.getCashValue());

        BigDecimal postEquityPct = percentOf(targetEquityValue, total);
        BigDecimal postBondPct = percentOf(targetBondValue, total);
        BigDecimal postCashPct = percentOf(targetCashValue, total);

        return Optional.of(new RebalanceProposalResult(
                account.getAccountId(),
                bands,
                total, account.getEquityValue(), account.getBondValue(), account.getCashValue(),
                currentEquityPct, currentBondPct, currentCashPct,
                total, targetEquityValue, targetBondValue, targetCashValue,
                postEquityPct, postBondPct, postCashPct,
                equityTrade, bondTrade, cashTrade));
    }

    private static BigDecimal percentOf(BigDecimal value, BigDecimal total) {
        return value.divide(total, PCT_SCALE, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }
}
