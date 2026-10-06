import numpy as np

from rebalancer_opt.impact_cost import impact_bps, impact_cost_dollars, total_impact_cost_dollars


def test_impact_bps_scales_with_sqrt_of_participation():
    adv = np.array([1_000_000.0])
    trade_1pct = np.array([10_000.0])
    trade_4pct = np.array([40_000.0])
    bps_1 = impact_bps(trade_1pct, adv)[0]
    bps_4 = impact_bps(trade_4pct, adv)[0]
    assert np.isclose(bps_4 / bps_1, 2.0, rtol=1e-9)


def test_impact_bps_is_zero_for_zero_trade():
    adv = np.array([1_000_000.0])
    assert impact_bps(np.array([0.0]), adv)[0] == 0.0


def test_impact_cost_is_nondecreasing_in_trade_size():
    adv = np.full(5, 5_000_000.0)
    small = np.full(5, 10_000.0)
    large = np.full(5, 100_000.0)
    assert np.all(impact_cost_dollars(large, adv) >= impact_cost_dollars(small, adv))


def test_impact_cost_scaled_by_fraction_of_adv_not_absolute_size():
    small_adv = np.array([1_000_000.0])
    large_adv = np.array([10_000_000.0])
    trade = np.array([100_000.0])
    cost_small_adv = total_impact_cost_dollars(trade, small_adv)
    cost_large_adv = total_impact_cost_dollars(trade, large_adv)
    assert cost_small_adv > cost_large_adv
