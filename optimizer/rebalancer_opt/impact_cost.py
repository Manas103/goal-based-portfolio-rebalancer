"""Square-root market impact cost, scaled by the trade's fraction of ADV.

cost_bps(trade, adv) = k * sqrt(|trade| / adv)

This is the standard square-root law (Almgren et al.), the same functional
form AQR's own trading-cost research argues for over a flat bps charge: cost
grows with the square root of participation rate, not linearly with trade
size. Dollar cost is cost_bps * |trade|, i.e. order size^1.5 in the
numerator, which is why the objective below is convex in trade size (a
power > 1 cost on a linear benefit is the standard way to make "trade more"
unattractive at the margin without a hard cap).
"""
from __future__ import annotations

import numpy as np

IMPACT_COEFFICIENT_BPS = 35.0  # bps of impact at 100% of ADV participation


def impact_bps(trade_dollars: np.ndarray, adv_dollars: np.ndarray) -> np.ndarray:
    participation = np.abs(trade_dollars) / adv_dollars
    return IMPACT_COEFFICIENT_BPS * np.sqrt(np.clip(participation, 0.0, None))


def impact_cost_dollars(trade_dollars: np.ndarray, adv_dollars: np.ndarray) -> np.ndarray:
    return impact_bps(trade_dollars, adv_dollars) * 1e-4 * np.abs(trade_dollars)


def total_impact_cost_dollars(trade_dollars: np.ndarray, adv_dollars: np.ndarray) -> float:
    return float(np.sum(impact_cost_dollars(trade_dollars, adv_dollars)))
