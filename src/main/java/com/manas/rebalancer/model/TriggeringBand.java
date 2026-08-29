package com.manas.rebalancer.model;

/**
 * Which rule fired to produce a rebalance proposal. An account can breach
 * more than one band at once (e.g. equity drift and the cash band together);
 * when that happens every band that fired is recorded on the ledger row
 * (see {@code RebalanceProposalEntity#getTriggeringBands()}), not just one.
 */
public enum TriggeringBand {
    /** |current equity % - target equity %| exceeds RebalancePolicy.DRIFT_BAND_PCT. */
    EQUITY_DRIFT,
    /** |current bond % - target bond %| exceeds RebalancePolicy.DRIFT_BAND_PCT. */
    BOND_DRIFT,
    /** current cash % is outside [RebalancePolicy.CASH_MIN_PCT, RebalancePolicy.CASH_MAX_PCT]. */
    CASH_BAND
}
