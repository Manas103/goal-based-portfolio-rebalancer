import numpy as np
import pytest

from rebalancer_opt.allocation import pro_rata_dollar_allocation, pro_rata_share_allocation
from rebalancer_opt.netting import gross_notional, net_notional, net_trade_per_security, netting_reduction_fraction
from rebalancer_opt.reference_oracle import net_trade_per_security_oracle, pro_rata_share_allocation_oracle


def test_net_notional_never_exceeds_gross_notional():
    rng = np.random.default_rng(1)
    sleeve_trades = {f"sleeve_{i}": rng.normal(0, 1e5, size=20) for i in range(4)}
    assert net_notional(sleeve_trades) <= gross_notional(sleeve_trades) + 1e-6


def test_perfectly_offsetting_trades_net_to_zero():
    sleeve_trades = {
        "a": np.array([100.0, -50.0, 0.0]),
        "b": np.array([-100.0, 50.0, 0.0]),
    }
    assert np.allclose(net_trade_per_security(sleeve_trades), 0.0)
    assert net_notional(sleeve_trades) == 0.0
    assert netting_reduction_fraction(sleeve_trades) == 1.0


def test_same_direction_trades_do_not_net_at_all():
    sleeve_trades = {
        "a": np.array([100.0, 50.0]),
        "b": np.array([30.0, 20.0]),
    }
    assert netting_reduction_fraction(sleeve_trades) == pytest.approx(0.0, abs=1e-9)


def test_netting_oracle_matches_real_implementation_exactly():
    rng = np.random.default_rng(2)
    for _ in range(20):
        sleeve_trades = {f"sleeve_{i}": rng.normal(0, 1e4, size=15) for i in range(4)}
        real = net_trade_per_security(sleeve_trades)
        oracle = net_trade_per_security_oracle(sleeve_trades)
        assert np.allclose(real, oracle, atol=1e-9)


def test_pro_rata_dollar_allocation_sums_to_the_sleeve_trade():
    rng = np.random.default_rng(3)
    account_aum = rng.uniform(1e5, 1e7, size=50)
    allocated = pro_rata_dollar_allocation(123456.78, account_aum)
    assert np.isclose(np.sum(allocated), 123456.78)


@pytest.mark.parametrize("trial_seed", range(30))
def test_pro_rata_share_allocation_conserves_shares_exactly(trial_seed):
    rng = np.random.default_rng(trial_seed)
    account_aum = rng.uniform(1e5, 1e7, size=50)
    shares = int(rng.integers(-5000, 5000))
    allocated = pro_rata_share_allocation(shares, account_aum)
    assert int(np.sum(allocated)) == shares


@pytest.mark.parametrize("trial_seed", range(30))
def test_pro_rata_allocation_matches_independent_oracle_exactly(trial_seed):
    rng = np.random.default_rng(1000 + trial_seed)
    account_aum = rng.uniform(1e5, 1e7, size=50)
    shares = int(rng.integers(-5000, 5000))
    real = pro_rata_share_allocation(shares, account_aum)
    oracle = pro_rata_share_allocation_oracle(shares, list(account_aum))
    assert list(real) == oracle


def test_pro_rata_allocation_handles_zero_shares():
    account_aum = np.array([1.0, 2.0, 3.0])
    allocated = pro_rata_share_allocation(0, account_aum)
    assert np.array_equal(allocated, np.array([0, 0, 0]))
