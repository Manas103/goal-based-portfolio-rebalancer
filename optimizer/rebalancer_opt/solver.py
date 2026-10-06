"""Per-sleeve convex trade optimizer: projected gradient descent.

Decision variable: `trade`, one dollar amount per security (positive = buy).
Objective: squared tracking error to the sleeve's target weights, plus the
square-root impact cost of trading (impact_cost.py), both convex in `trade`.

A generic NLP solver (scipy's `trust-constr` and `SLSQP`, both tried) took
well over a minute per sleeve on this machine even with analytic gradients
and vectorized constraints (see README Findings), which is not tractable
for a 4-sleeves x 12-periods backtest. This project's 9 constraints have a
specific structure that a generic solver does not exploit: 4 of them
(long-only, max security weight, max active weight, ADV participation) are
elementwise box constraints on `trade` and can be intersected into one
per-security [lo, hi] box directly; the rest are one linear inequality each
over a small group of securities (a sector, the whole sleeve, or a named
subset). That structure makes alternating projection practical: take a
gradient step on the unconstrained objective, then project back onto each
constraint's feasible set in turn (box, then each sector cap, then the
turnover ball, then the cash half-space, then factor neutrality), repeat.
This is a heuristic (not a certified global optimum the way an interior-
point QP solver would give), so the result is independently re-checked
against every constraint class's own `check()` after the loop, with a
proportional-shrink fallback if any numerical residual remains.

`MinTradeSizeConstraint` (the round-lot floor) is enforced by post-
processing only, same reasoning as before: it is an all-or-nothing
condition, not a convex set to project onto.
"""
from __future__ import annotations

import numpy as np

from .constraints import ALL_CONSTRAINTS, MinTradeSizeConstraint, SleeveContext
from .impact_cost import IMPACT_COEFFICIENT_BPS

TRACKING_ERROR_SCALE = 0.2
LEARNING_RATE = 0.5
GRADIENT_STEPS = 60
PROJECTION_ROUNDS_PER_STEP = 3


def _objective_gradient(trade: np.ndarray, ctx: SleeveContext) -> np.ndarray:
    post_weights = ctx.current_weights + trade / ctx.aum_dollars
    d_tracking = 2.0 * (post_weights - ctx.target_weights)
    participation = np.clip(np.abs(trade) / ctx.adv_dollars, 1e-12, None)
    d_impact = 1.5 * IMPACT_COEFFICIENT_BPS * 1e-4 * np.sign(trade) * np.sqrt(participation)
    return TRACKING_ERROR_SCALE * d_tracking + d_impact


def _elementwise_box(ctx: SleeveContext) -> tuple[np.ndarray, np.ndarray]:
    n = len(ctx.current_weights)
    lo = np.full(n, -np.inf)
    hi = np.full(n, np.inf)

    # LongOnlyConstraint: post >= 0
    lo = np.maximum(lo, -ctx.current_weights * ctx.aum_dollars)
    # MaxSecurityWeightConstraint: post <= 0.08
    hi = np.minimum(hi, (0.08 - ctx.current_weights) * ctx.aum_dollars)
    # MaxActiveWeightConstraint: |post - target| <= 0.03
    lo = np.maximum(lo, (ctx.target_weights - 0.03 - ctx.current_weights) * ctx.aum_dollars)
    hi = np.minimum(hi, (ctx.target_weights + 0.03 - ctx.current_weights) * ctx.aum_dollars)
    # AdvParticipationConstraint: |trade| <= 10% of ADV
    bound = 0.10 * ctx.adv_dollars
    lo = np.maximum(lo, -bound)
    hi = np.minimum(hi, bound)

    return lo, hi


def _project(trade: np.ndarray, ctx: SleeveContext, lo: np.ndarray, hi: np.ndarray) -> np.ndarray:
    t = np.clip(trade, lo, hi)

    for sector in range(ctx.num_sectors):
        mask = ctx.sector_of == sector
        current_sector_weight = float(np.sum(ctx.current_weights[mask]))
        budget = (0.30 - current_sector_weight) * ctx.aum_dollars
        sector_sum = float(np.sum(t[mask]))
        if sector_sum > budget > 0:
            t[mask] = t[mask] * (budget / sector_sum)
        elif sector_sum > budget:
            t[mask] = np.minimum(t[mask], 0.0)

    turnover_cap = 0.25 * ctx.aum_dollars
    total_turnover = float(np.sum(np.abs(t)))
    if total_turnover > turnover_cap > 0:
        t = t * (turnover_cap / total_turnover)

    cash_cap = 0.02 * ctx.aum_dollars
    net_buy = float(np.sum(t))
    if net_buy > cash_cap:
        buys = t > 0
        buy_total = float(np.sum(t[buys]))
        if buy_total > 0:
            excess = net_buy - cash_cap
            t[buys] = t[buys] * max(0.0, 1.0 - excess / buy_total)

    factor_mask = ctx.sector_of == 0
    factor_cap = 0.005 * ctx.aum_dollars
    factor_exposure = float(np.sum(t[factor_mask]))
    if abs(factor_exposure) > factor_cap and factor_mask.any():
        scale = factor_cap / abs(factor_exposure)
        t[factor_mask] = t[factor_mask] * scale

    return np.clip(t, lo, hi)


def solve_sleeve_trade(ctx: SleeveContext) -> np.ndarray:
    lo, hi = _elementwise_box(ctx)
    trade = np.clip(np.zeros_like(ctx.current_weights), lo, hi)

    # Step scale: the tracking-error term alone has gradient
    # TRACKING_ERROR_SCALE * 2 * (post - target); a step of
    # aum / (TRACKING_ERROR_SCALE * 2) along that gradient would close the
    # whole tracking gap in a single step if impact cost did not push back.
    # LEARNING_RATE < 1 takes a fraction of that step each iteration, so the
    # loop converges geometrically rather than overshooting into the next
    # projection round.
    step_scale = LEARNING_RATE * ctx.aum_dollars / (2.0 * TRACKING_ERROR_SCALE)

    for _ in range(GRADIENT_STEPS):
        grad = _objective_gradient(trade, ctx)
        trade = trade - step_scale * grad
        for _ in range(PROJECTION_ROUNDS_PER_STEP):
            trade = _project(trade, ctx, lo, hi)

    floor_constraint = MinTradeSizeConstraint()
    small = np.abs(trade) < floor_constraint.FLOOR_DOLLARS
    trade = np.where(small, 0.0, trade)

    for constraint in ALL_CONSTRAINTS:
        attempts = 0
        while not constraint.check(trade, ctx) and attempts < 50:
            trade = trade * 0.995
            attempts += 1

    return trade
