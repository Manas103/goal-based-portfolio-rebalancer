from rebalancer_opt.simulate import run_simulation


def test_simulation_runs_and_reports_all_claim_fields():
    result = run_simulation(seed=7)
    assert result["periods"] == 12
    assert result["accounts"] == 200
    assert "baseline_turnover_annualized_pct" in result
    assert "optimized_turnover_annualized_pct" in result
    assert "netting_reduction_pct" in result
    assert result["allocation_checks_performed"] > 0


def test_optimized_turnover_is_not_higher_than_baseline():
    result = run_simulation(seed=11)
    assert result["optimized_turnover_annualized_pct"] <= result["baseline_turnover_annualized_pct"]
