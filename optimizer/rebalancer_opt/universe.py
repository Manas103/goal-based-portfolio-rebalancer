"""Synthetic security universe and sleeve/account population shared by the optimizer.

Four sleeves share one 60-security universe so that offsetting trades across
sleeves are possible (the netting claim only means something if sleeves can
actually disagree about the same security).
"""
from __future__ import annotations

import numpy as np

SLEEVES = ["US_LARGE_CAP", "US_SMALL_CAP", "INTL_DEVELOPED", "EMERGING_MARKETS"]
NUM_SECURITIES = 60
NUM_SECTORS = 6
ACCOUNTS_PER_SLEEVE = 50
TOTAL_ACCOUNTS = ACCOUNTS_PER_SLEEVE * len(SLEEVES)


def security_sectors(seed: int = 7) -> np.ndarray:
    rng = np.random.default_rng(seed)
    return rng.integers(0, NUM_SECTORS, size=NUM_SECURITIES)


def average_daily_volume_dollars(seed: int = 11) -> np.ndarray:
    """ADV in dollars per security, log-normal to get a realistic fat tail."""
    rng = np.random.default_rng(seed)
    return np.exp(rng.normal(loc=16.0, scale=1.1, size=NUM_SECURITIES))


def _cap_and_redistribute(weights: np.ndarray, cap: float, max_rounds: int = 50) -> np.ndarray:
    """Water-filling: repeatedly clip entries above `cap` and spread the
    excess proportionally across entries still below it, so the result sums
    to the same total and no entry exceeds `cap` (unless every entry is
    already at the cap, in which case the total itself exceeds len*cap).
    """
    w = weights.copy()
    total = float(np.sum(w))
    for _ in range(max_rounds):
        over = w > cap
        if not np.any(over):
            break
        excess = float(np.sum(w[over] - cap))
        w[over] = cap
        under = ~over
        under_total = float(np.sum(w[under]))
        if under_total <= 0 or excess <= 1e-15:
            break
        w[under] = w[under] + excess * (w[under] / under_total)
    current_total = float(np.sum(w))
    if current_total > 0:
        w = w * (total / current_total)
    return w


def sleeve_target_weights(seed: int = 3) -> dict[str, np.ndarray]:
    """One target-weight vector per sleeve, each summing to 1.0, built
    hierarchically (sector weights, then securities within each sector) and
    capped at every level, so every target is itself feasible against
    MaxSectorWeightConstraint (30%) and MaxSecurityWeightConstraint (8%)
    before any trade is ever computed. A target that already breaches the
    institution's own caps would make those two constraint classes
    unsatisfiable by construction, which would not be a finding about
    liquidity or drift, just an unfair starting point.

    All 60 securities are held by every sleeve (no sparsity mask): sleeves
    differ in how much weight they put on each security, not in which
    securities they are allowed to hold, which is what makes cross-sleeve
    netting a property of overlapping, disagreeing weights rather than of
    disjoint universes.
    """
    rng = np.random.default_rng(seed)
    sector_of = security_sectors()
    weights: dict[str, np.ndarray] = {}

    for sleeve in SLEEVES:
        sector_raw = rng.dirichlet(np.full(NUM_SECTORS, 8.0))
        sector_weight = _cap_and_redistribute(sector_raw, cap=0.28)

        full = np.zeros(NUM_SECURITIES)
        for sector in range(NUM_SECTORS):
            idx = np.where(sector_of == sector)[0]
            within_raw = rng.dirichlet(np.full(len(idx), 5.0))
            within_capped = _cap_and_redistribute(within_raw, cap=0.075 / max(sector_weight[sector], 1e-9))
            full[idx] = within_capped * sector_weight[sector]

        weights[sleeve] = full / np.sum(full)

    return weights


def account_aum_dollars(seed: int = 5) -> dict[str, np.ndarray]:
    """AUM per account within each sleeve (lognormal, 50 accounts/sleeve)."""
    rng = np.random.default_rng(seed)
    out: dict[str, np.ndarray] = {}
    for sleeve in SLEEVES:
        out[sleeve] = np.exp(rng.normal(loc=12.5, scale=0.6, size=ACCOUNTS_PER_SLEEVE))
    return out
