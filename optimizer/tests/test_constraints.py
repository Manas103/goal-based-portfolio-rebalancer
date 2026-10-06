import numpy as np
import pytest

from rebalancer_opt.constraints import ALL_CONSTRAINTS, SleeveContext
from rebalancer_opt.universe import NUM_SECTORS, security_sectors, average_daily_volume_dollars, sleeve_target_weights


def _base_ctx():
    sector_of = security_sectors()
    adv = average_daily_volume_dollars()
    targets = sleeve_target_weights()
    w = targets["US_LARGE_CAP"]
    return SleeveContext(
        current_weights=w.copy(),
        target_weights=w.copy(),
        sector_of=sector_of,
        adv_dollars=adv,
        aum_dollars=5e7,
        num_sectors=NUM_SECTORS,
    )


def test_exactly_nine_constraint_classes():
    assert len(ALL_CONSTRAINTS) == 9
    names = {c.name for c in ALL_CONSTRAINTS}
    assert len(names) == 9


@pytest.mark.parametrize("constraint", ALL_CONSTRAINTS, ids=lambda c: c.name)
def test_zero_trade_always_satisfies_every_constraint(constraint):
    ctx = _base_ctx()
    zero_trade = np.zeros_like(ctx.current_weights)
    assert constraint.check(zero_trade, ctx)


def test_long_only_rejects_a_trade_that_oversells_a_security():
    ctx = _base_ctx()
    trade = np.zeros_like(ctx.current_weights)
    trade[0] = -(ctx.current_weights[0] * ctx.aum_dollars) - 1000.0
    constraint = next(c for c in ALL_CONSTRAINTS if c.name == "long_only")
    assert not constraint.check(trade, ctx)


def test_max_security_weight_rejects_an_oversized_buy():
    ctx = _base_ctx()
    trade = np.zeros_like(ctx.current_weights)
    trade[0] = 0.5 * ctx.aum_dollars
    constraint = next(c for c in ALL_CONSTRAINTS if c.name == "max_security_weight")
    assert not constraint.check(trade, ctx)


def test_max_sector_weight_rejects_concentrating_one_sector():
    ctx = _base_ctx()
    trade = np.zeros_like(ctx.current_weights)
    sector_0_idx = np.where(ctx.sector_of == 0)[0]
    trade[sector_0_idx] = 0.1 * ctx.aum_dollars
    constraint = next(c for c in ALL_CONSTRAINTS if c.name == "max_sector_weight")
    assert not constraint.check(trade, ctx)


def test_max_active_weight_rejects_drifting_far_from_target():
    ctx = _base_ctx()
    trade = np.zeros_like(ctx.current_weights)
    trade[0] = 0.1 * ctx.aum_dollars
    constraint = next(c for c in ALL_CONSTRAINTS if c.name == "max_active_weight")
    assert not constraint.check(trade, ctx)


def test_turnover_budget_rejects_a_full_portfolio_turn():
    ctx = _base_ctx()
    n = len(ctx.current_weights)
    trade = np.full(n, 0.5 * ctx.aum_dollars / n)
    constraint = next(c for c in ALL_CONSTRAINTS if c.name == "turnover_budget")
    assert not constraint.check(trade, ctx)


def test_cash_floor_rejects_net_buying_beyond_the_cap():
    ctx = _base_ctx()
    n = len(ctx.current_weights)
    trade = np.full(n, 0.1 * ctx.aum_dollars / n)
    constraint = next(c for c in ALL_CONSTRAINTS if c.name == "cash_floor")
    assert not constraint.check(trade, ctx)


def test_adv_participation_rejects_an_oversized_single_trade():
    ctx = _base_ctx()
    trade = np.zeros_like(ctx.current_weights)
    trade[0] = ctx.adv_dollars[0] * 0.9
    constraint = next(c for c in ALL_CONSTRAINTS if c.name == "adv_participation")
    assert not constraint.check(trade, ctx)


def test_min_trade_size_rejects_a_dust_trade():
    ctx = _base_ctx()
    trade = np.zeros_like(ctx.current_weights)
    trade[0] = 50.0
    constraint = next(c for c in ALL_CONSTRAINTS if c.name == "min_trade_size")
    assert not constraint.check(trade, ctx)


def test_factor_neutrality_rejects_a_one_sided_sector_zero_tilt():
    ctx = _base_ctx()
    trade = np.zeros_like(ctx.current_weights)
    sector_0_idx = np.where(ctx.sector_of == 0)[0]
    trade[sector_0_idx] = 0.02 * ctx.aum_dollars
    constraint = next(c for c in ALL_CONSTRAINTS if c.name == "factor_neutrality")
    assert not constraint.check(trade, ctx)
