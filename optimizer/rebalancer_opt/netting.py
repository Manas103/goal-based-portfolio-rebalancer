"""Netting offsetting trades across the 4 sleeves before anything reaches the market.

Input: a dict sleeve -> trade vector (dollars per security, positive = buy),
one vector per sleeve from solver.solve_sleeve_trade. Netting sums the four
vectors security by security; the market only ever sees the net. Gross
notional is what would have been sent to the market with no netting (each
sleeve executes its own trade independently); net notional is what is
actually sent once offsetting buys and sells in the same security cancel.
"""
from __future__ import annotations

import numpy as np


def gross_notional(sleeve_trades: dict[str, np.ndarray]) -> float:
    return float(sum(np.sum(np.abs(t)) for t in sleeve_trades.values()))


def net_trade_per_security(sleeve_trades: dict[str, np.ndarray]) -> np.ndarray:
    vectors = list(sleeve_trades.values())
    return np.sum(vectors, axis=0)


def net_notional(sleeve_trades: dict[str, np.ndarray]) -> float:
    return float(np.sum(np.abs(net_trade_per_security(sleeve_trades))))


def netting_reduction_fraction(sleeve_trades: dict[str, np.ndarray]) -> float:
    gross = gross_notional(sleeve_trades)
    if gross == 0.0:
        return 0.0
    return 1.0 - (net_notional(sleeve_trades) / gross)
