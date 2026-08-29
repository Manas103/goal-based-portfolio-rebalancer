package com.manas.rebalancer.engine;

import java.math.BigDecimal;

/**
 * The fixed, institution-wide policy every account in this repository is
 * evaluated against. Two deliberate simplifications, stated honestly:
 *
 * <ul>
 *   <li>The drift band width (5 percentage points) is one number applied to
 *   every account's equity and bond target, not a per-account field. A real
 *   advisory shop's investment policy statement (IPS) typically sets one
 *   drift tolerance for the whole book of a given account type; letting
 *   every account pick its own band would be more flexible but would also
 *   make "40 of 40 seeded breaches produce the correct trade set" a claim
 *   about 40 different rules instead of one rule applied 40 times, which is
 *   a weaker thing to measure.</li>
 *   <li>The cash band ([2%, 15%]) is an absolute range, not relative to an
 *   account's own target cash weight. It models a portfolio-wide minimum
 *   liquidity buffer and a maximum idle-cash ceiling, both compliance
 *   concerns that apply regardless of what a given goal's target happens to
 *   be, which is why the resume bullet and this repository treat it as a
 *   trigger independent of the two drift bands rather than a third drift
 *   band.</li>
 * </ul>
 */
public final class RebalancePolicy {

    /** Percentage points; a drift with absolute value strictly greater than this breaches. */
    public static final BigDecimal DRIFT_BAND_PCT = new BigDecimal("5");

    public static final BigDecimal CASH_MIN_PCT = new BigDecimal("2");
    public static final BigDecimal CASH_MAX_PCT = new BigDecimal("15");

    /**
     * Bumped whenever the rebalancing rule itself changes (band widths, which
     * asset absorbs the rounding remainder, rebalance-to-target vs
     * rebalance-to-band-edge). Every ledger row records the version that
     * produced it, so a policy change never silently reinterprets an old
     * proposal.
     */
    public static final String RULE_VERSION = "drift-band-v1";

    private RebalancePolicy() {
    }
}
