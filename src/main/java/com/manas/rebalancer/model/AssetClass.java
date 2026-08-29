package com.manas.rebalancer.model;

/**
 * The three asset classes this repository models for a retail goal-based
 * advisory account. Equity and bond are the two classes actually traded to
 * rebalance; cash is the residual buffer every goal-based account holds
 * against its own target cash weight and against a portfolio-wide minimum
 * liquidity / maximum idle-cash band (see {@code RebalancePolicy}).
 */
public enum AssetClass {
    EQUITY,
    BOND,
    CASH
}
