import numpy as np
import pytest

from rebalancer_opt.constraints import ALL_CONSTRAINTS, SleeveContext
from rebalancer_opt.solver import solve_sleeve_trade
from rebalancer_opt.universe import NUM_SECTORS, average_daily_volume_dollars, security_sectors, sleeve_target_weights


@pytest.mark.parametrize("sleeve_seed", [1, 2, 3, 4, 5])
def test_solved_trade_satisfies_all_nine_constraints_under_random_drift(sleeve_seed):
    rng = np.random.default_rng(sleeve_seed)
    sector_of = security_sectors()
    adv = average_daily_volume_dollars()
    targets = sleeve_target_weights()
    target = targets["US_LARGE_CAP"]

    drift = 1.0 + rng.normal(0.0, 0.08, size=len(target))
    current = np.clip(target * drift, 1e-6, None)
    current = current / np.sum(current)

    ctx = SleeveContext(
        current_weights=current,
        target_weights=target,
        sector_of=sector_of,
        adv_dollars=adv,
        aum_dollars=5e7,
        num_sectors=NUM_SECTORS,
    )
    trade = solve_sleeve_trade(ctx)

    for constraint in ALL_CONSTRAINTS:
        assert constraint.check(trade, ctx), f"{constraint.name} violated for seed {sleeve_seed}"


def test_solver_moves_toward_target_not_away_from_it():
    sector_of = security_sectors()
    adv = average_daily_volume_dollars()
    targets = sleeve_target_weights()
    target = targets["US_LARGE_CAP"]

    rng = np.random.default_rng(99)
    current = target * (1.0 + rng.normal(0.0, 0.05, size=len(target)))
    current = np.clip(current, 1e-6, None)
    current = current / np.sum(current)

    ctx = SleeveContext(
        current_weights=current, target_weights=target, sector_of=sector_of,
        adv_dollars=adv, aum_dollars=5e7, num_sectors=NUM_SECTORS,
    )
    trade = solve_sleeve_trade(ctx)
    post = current + trade / ctx.aum_dollars
    error_before = np.sum((current - target) ** 2)
    error_after = np.sum((post - target) ** 2)
    assert error_after < error_before


def test_solver_is_a_no_op_when_already_at_target():
    sector_of = security_sectors()
    adv = average_daily_volume_dollars()
    targets = sleeve_target_weights()
    target = targets["US_LARGE_CAP"]
    ctx = SleeveContext(
        current_weights=target.copy(), target_weights=target.copy(), sector_of=sector_of,
        adv_dollars=adv, aum_dollars=5e7, num_sectors=NUM_SECTORS,
    )
    trade = solve_sleeve_trade(ctx)
    assert np.sum(np.abs(trade)) < 1.0
