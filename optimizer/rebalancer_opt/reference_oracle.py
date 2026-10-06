"""Independently coded reference implementations of netting and pro-rata allocation.

Both are written as plain Python loops over scalars, deliberately not using
numpy vectorization, so the only thing shared with the real implementation
(netting.py, allocation.py) is the specification, not the code shape. They
are diffed exactly against the real implementation in
tests/test_reference_oracle.py.
"""
from __future__ import annotations


def net_trade_per_security_oracle(sleeve_trades: dict) -> list[float]:
    sleeves = list(sleeve_trades.keys())
    n = len(sleeve_trades[sleeves[0]])
    net = [0.0] * n
    for sleeve in sleeves:
        vector = sleeve_trades[sleeve]
        for i in range(n):
            net[i] += float(vector[i])
    return net


def pro_rata_share_allocation_oracle(sleeve_trade_shares: int, account_aum: list) -> list[int]:
    n = len(account_aum)
    sign = 1 if sleeve_trade_shares >= 0 else -1
    total_shares = abs(int(sleeve_trade_shares))
    total_aum = 0.0
    for aum in account_aum:
        total_aum += float(aum)

    raw = []
    for aum in account_aum:
        raw.append(total_shares * (float(aum) / total_aum))

    floors = []
    assigned = 0
    for value in raw:
        f = int(value // 1)
        floors.append(f)
        assigned += f

    remainder = total_shares - assigned
    fractions = []
    for i in range(n):
        fractions.append((raw[i] - floors[i], i))
    fractions.sort(key=lambda pair: -pair[0])

    result = list(floors)
    for k in range(remainder):
        _, idx = fractions[k]
        result[idx] += 1

    return [sign * r for r in result]
