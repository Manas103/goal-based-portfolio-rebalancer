"""Pro rata fill allocation of one sleeve's trade across its accounts.

A sleeve's trade in one security is a single dollar amount; it must be
split across that sleeve's accounts in proportion to each account's AUM
share of the sleeve, and the split must sum back to the sleeve trade
exactly in dollars (and, once converted to shares at a price, exactly in
shares). Naive per-account rounding does not do this (see the
allocation-affirmation-workflow lesson in the README): rounding each
account's share count independently can over- or under-allocate the total
by a few shares. This module uses the largest-remainder method instead.
"""
from __future__ import annotations

import numpy as np


def pro_rata_dollar_allocation(sleeve_trade_dollars: float, account_aum: np.ndarray) -> np.ndarray:
    weights = account_aum / np.sum(account_aum)
    return sleeve_trade_dollars * weights


def pro_rata_share_allocation(sleeve_trade_shares: int, account_aum: np.ndarray) -> np.ndarray:
    """Largest-remainder allocation of an integer share trade across accounts.

    Guarantees sum(result) == sleeve_trade_shares exactly, unlike independent
    per-account rounding of each account's proportional share.
    """
    n = len(account_aum)
    sign = 1 if sleeve_trade_shares >= 0 else -1
    total_shares = abs(sleeve_trade_shares)
    weights = account_aum / np.sum(account_aum)
    raw = total_shares * weights
    floors = np.floor(raw).astype(np.int64)
    remainder = total_shares - int(np.sum(floors))
    fractional = raw - floors
    order = np.argsort(-fractional)
    result = floors.copy()
    for i in range(remainder):
        result[order[i]] += 1
    return sign * result
