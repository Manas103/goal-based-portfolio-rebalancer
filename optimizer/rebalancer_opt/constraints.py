"""The 9 constraint classes applied to one sleeve's trade-optimization problem.

Each class's `check(trade, ctx)` is the specification: given a candidate
trade vector (dollars per security, positive = buy) and the sleeve's
context, is this constraint satisfied. `solver.py` enforces the same 9
specifications through its own projection logic rather than calling
`check` to build the solve; `check` is the independent, after-the-fact
judge, exercised directly in `tests/test_constraints.py` and by every
test in `tests/test_solver.py` that asserts a solved trade satisfies all
nine.

Keeping each constraint as its own class, rather than one big function, is
what makes "9 constraint classes" a checkable claim: `ALL_CONSTRAINTS`
below has a test asserting its length is exactly 9 and that each one
individually rejects a trade vector constructed to violate only it.
"""
from __future__ import annotations

from dataclasses import dataclass

import numpy as np


@dataclass
class SleeveContext:
    current_weights: np.ndarray
    target_weights: np.ndarray
    sector_of: np.ndarray
    adv_dollars: np.ndarray
    aum_dollars: float
    num_sectors: int


class Constraint:
    name: str = "base"

    def check(self, trade: np.ndarray, ctx: SleeveContext) -> bool:
        raise NotImplementedError


class LongOnlyConstraint(Constraint):
    """No security's post-trade weight may go negative."""

    name = "long_only"

    def check(self, trade, ctx):
        post = ctx.current_weights + trade / ctx.aum_dollars
        return bool(np.all(post >= -1e-9))


class MaxSecurityWeightConstraint(Constraint):
    """No single security may exceed 8% post-trade weight."""

    name = "max_security_weight"
    CAP = 0.08

    def check(self, trade, ctx):
        post = ctx.current_weights + trade / ctx.aum_dollars
        return bool(np.all(post <= self.CAP + 1e-9))


class MaxSectorWeightConstraint(Constraint):
    """No sector may exceed 30% post-trade weight."""

    name = "max_sector_weight"
    CAP = 0.30

    def check(self, trade, ctx):
        post = ctx.current_weights + trade / ctx.aum_dollars
        for sector in range(ctx.num_sectors):
            mask = ctx.sector_of == sector
            if np.sum(post[mask]) > self.CAP + 1e-9:
                return False
        return True


class MaxActiveWeightConstraint(Constraint):
    """No security's post-trade weight may deviate from target by more than 3pp."""

    name = "max_active_weight"
    CAP = 0.03

    def check(self, trade, ctx):
        post = ctx.current_weights + trade / ctx.aum_dollars
        return bool(np.all(np.abs(post - ctx.target_weights) <= self.CAP + 1e-9))


class TurnoverBudgetConstraint(Constraint):
    """Total one-way turnover this rebalance may not exceed 25% of AUM."""

    name = "turnover_budget"
    CAP_FRACTION = 0.25

    def check(self, trade, ctx):
        return bool(np.sum(np.abs(trade)) <= self.CAP_FRACTION * ctx.aum_dollars + 1e-6)


class CashFloorConstraint(Constraint):
    """Net buying this rebalance may not spend more than 2% of AUM in net cash."""

    name = "cash_floor"
    CAP_FRACTION = 0.02

    def check(self, trade, ctx):
        return bool(np.sum(trade) <= self.CAP_FRACTION * ctx.aum_dollars + 1e-6)


class AdvParticipationConstraint(Constraint):
    """No single trade may exceed 10% of that security's ADV (liquidity cap)."""

    name = "adv_participation"
    CAP_FRACTION = 0.10

    def check(self, trade, ctx):
        return bool(np.all(np.abs(trade) <= self.CAP_FRACTION * ctx.adv_dollars + 1e-6))


class MinTradeSizeConstraint(Constraint):
    """Round-lot floor: a nonzero trade must be at least $500 (post-hoc check only;
    the solver is not asked to enforce a non-convex all-or-nothing minimum, it is
    checked and rounded away in post-processing, see solver.py)."""

    name = "min_trade_size"
    FLOOR_DOLLARS = 500.0

    def check(self, trade, ctx):
        nonzero = trade[np.abs(trade) > 1e-6]
        return bool(np.all(np.abs(nonzero) >= self.FLOOR_DOLLARS - 1e-6))


class FactorNeutralityConstraint(Constraint):
    """Net trade must be sector-neutral in aggregate dollar terms within +/-0.5% of AUM
    for sector 0, used here as the stand-in 'factor' exposure the sleeve must not
    build up or run down through trading."""

    name = "factor_neutrality"
    CAP_FRACTION = 0.005

    def check(self, trade, ctx):
        exposure = np.sum(trade[ctx.sector_of == 0])
        return bool(abs(exposure) <= self.CAP_FRACTION * ctx.aum_dollars + 1e-6)


ALL_CONSTRAINTS: list[Constraint] = [
    LongOnlyConstraint(),
    MaxSecurityWeightConstraint(),
    MaxSectorWeightConstraint(),
    MaxActiveWeightConstraint(),
    TurnoverBudgetConstraint(),
    CashFloorConstraint(),
    AdvParticipationConstraint(),
    MinTradeSizeConstraint(),
    FactorNeutralityConstraint(),
]

assert len(ALL_CONSTRAINTS) == 9
