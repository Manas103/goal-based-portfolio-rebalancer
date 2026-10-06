"""Multi-period simulation measuring the netting and turnover claims.

12 monthly rebalances, 4 sleeves, 50 accounts/sleeve (200 total), 60 shared
securities. Each period, prices move randomly, which drifts every sleeve's
weights away from target; then two parallel approaches rebalance:

- baseline: each sleeve trades the whole way back to target independently,
  no impact-cost awareness, no cross-sleeve netting. This is the "calendar
  rebalancing, sleeves rebalanced independently" baseline the resume's
  turnover claim is stated against.
- optimized: solver.solve_sleeve_trade (9 constraint classes, square-root
  impact cost) per sleeve, then netting.py nets the four sleeves' trades
  before anything is sent to the market.

Both approaches update sleeve weights using their own full solved trade
(internal crossing between sleeves is economically real even when the net
market order is smaller); only the optimized approach's market-facing
notional is reduced by netting.

Each period, every sleeve also gets its own small, independent "tactical
tilt" on top of its long-run strategic target (modeling each sleeve's own
signal moving slightly differently period to period). Without this, every
sleeve's correction direction for a given security would be driven almost
entirely by that period's common market-wide price move (the same shock
hits every sleeve's holding of the same security), which would make
offsetting cross-sleeve trades a near-impossibility by construction rather
than a measured property of the netting logic; see README Findings for the
first run that actually demonstrated this (zero measurable netting until
the tilt was added).
"""
from __future__ import annotations

import numpy as np

from .allocation import pro_rata_share_allocation
from .constraints import SleeveContext
from .netting import gross_notional, net_notional
from .solver import solve_sleeve_trade
from .universe import (
    ACCOUNTS_PER_SLEEVE,
    NUM_SECTORS,
    SLEEVES,
    account_aum_dollars,
    average_daily_volume_dollars,
    security_sectors,
    sleeve_target_weights,
)

NUM_PERIODS = 12
MONTHLY_RETURN_VOL = 0.06
TACTICAL_TILT_VOL = 0.05


def run_simulation(seed: int = 101) -> dict:
    rng = np.random.default_rng(seed)
    sector_of = security_sectors()
    adv = average_daily_volume_dollars()
    targets = sleeve_target_weights()
    account_aum = account_aum_dollars()

    sleeve_aum = {s: float(np.sum(account_aum[s])) for s in SLEEVES}
    current_weights = {s: targets[s].copy() for s in SLEEVES}

    baseline_notional_total = 0.0
    optimized_gross_notional_total = 0.0
    optimized_net_notional_total = 0.0
    total_aum_sum = 0.0
    allocation_checks = 0

    for period in range(NUM_PERIODS):
        returns = rng.normal(0.0, MONTHLY_RETURN_VOL, size=len(sector_of))
        price_relative = 1.0 + returns

        baseline_trades = {}
        optimized_trades = {}

        for sleeve in SLEEVES:
            drifted = current_weights[sleeve] * price_relative
            drifted = drifted / np.sum(drifted)
            aum = sleeve_aum[sleeve]

            tilt = rng.normal(0.0, TACTICAL_TILT_VOL, size=len(sector_of)) * targets[sleeve]
            tilt = tilt - np.mean(tilt)
            period_target = np.clip(targets[sleeve] + tilt, 0.0, None)
            period_target = period_target / np.sum(period_target)

            baseline_trade = (period_target - drifted) * aum
            baseline_trades[sleeve] = baseline_trade

            ctx = SleeveContext(
                current_weights=drifted,
                target_weights=period_target,
                sector_of=sector_of,
                adv_dollars=adv,
                aum_dollars=aum,
                num_sectors=NUM_SECTORS,
            )
            opt_trade = solve_sleeve_trade(ctx)
            optimized_trades[sleeve] = opt_trade

            current_weights[sleeve] = drifted + opt_trade / aum
            current_weights[sleeve] = current_weights[sleeve] / np.sum(current_weights[sleeve])

            total_aum_sum += aum

        baseline_notional_total += sum(float(np.sum(np.abs(t))) for t in baseline_trades.values())
        optimized_gross_notional_total += gross_notional(optimized_trades)
        optimized_net_notional_total += net_notional(optimized_trades)

        for sleeve in SLEEVES:
            trade = optimized_trades[sleeve]
            aum_per_account = account_aum[sleeve]
            security_idx = int(np.argmax(np.abs(trade)))
            price = 100.0
            shares = int(round(trade[security_idx] / price))
            if shares != 0:
                allocated = pro_rata_share_allocation(shares, aum_per_account)
                assert int(np.sum(allocated)) == shares
                allocation_checks += 1

    avg_total_aum_per_period = total_aum_sum / NUM_PERIODS
    baseline_turnover = baseline_notional_total / avg_total_aum_per_period
    optimized_turnover = optimized_net_notional_total / avg_total_aum_per_period
    netting_reduction = 1.0 - (optimized_net_notional_total / optimized_gross_notional_total)

    return {
        "periods": NUM_PERIODS,
        "accounts": ACCOUNTS_PER_SLEEVE * len(SLEEVES),
        "baseline_turnover_annualized_pct": baseline_turnover * 100.0,
        "optimized_turnover_annualized_pct": optimized_turnover * 100.0,
        "netting_reduction_pct": netting_reduction * 100.0,
        "optimized_gross_notional": optimized_gross_notional_total,
        "optimized_net_notional": optimized_net_notional_total,
        "allocation_checks_performed": allocation_checks,
    }


if __name__ == "__main__":
    import json

    print(json.dumps(run_simulation(), indent=2))
