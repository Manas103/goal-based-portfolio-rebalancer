package com.manas.rebalancer.service;

import com.manas.rebalancer.engine.DriftBandRebalancingEngine;
import com.manas.rebalancer.engine.RebalancePolicy;
import com.manas.rebalancer.engine.RebalanceProposalResult;
import com.manas.rebalancer.model.GoalAccountEntity;
import com.manas.rebalancer.model.RebalanceProposalEntity;
import com.manas.rebalancer.repository.RebalanceProposalRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Wires the real engine to the append-only ledger. This is the only class in
 * the repository that writes a {@link RebalanceProposalEntity}; nothing else
 * touches {@link RebalanceProposalRepository#save}.
 *
 * <p>A proposal is a recommendation, not an executed trade: evaluating an
 * account never mutates that account's own holdings. A real advisory
 * platform would apply the trade only after a human or a downstream
 * execution system acted on the proposal, which is outside this
 * repository's scope (see the README's Limitations).
 */
@Service
public class RebalancingService {

    private final DriftBandRebalancingEngine engine;
    private final RebalanceProposalRepository proposalRepository;

    public RebalancingService(DriftBandRebalancingEngine engine, RebalanceProposalRepository proposalRepository) {
        this.engine = engine;
        this.proposalRepository = proposalRepository;
    }

    public List<RebalanceProposalEntity> evaluateAndRecordAll(List<GoalAccountEntity> accounts) {
        List<RebalanceProposalEntity> written = new ArrayList<>();
        Instant now = Instant.now();
        for (GoalAccountEntity account : accounts) {
            engine.evaluate(account).ifPresent(result -> written.add(proposalRepository.save(toEntity(result, now))));
        }
        return written;
    }

    private static RebalanceProposalEntity toEntity(RebalanceProposalResult r, Instant proposedAt) {
        return new RebalanceProposalEntity(
                r.accountId(), r.triggeringBandsAsString(), RebalancePolicy.RULE_VERSION, proposedAt,
                r.preTotalValue(), r.preEquityValue(), r.preBondValue(), r.preCashValue(),
                r.preEquityPct(), r.preBondPct(), r.preCashPct(),
                r.postTotalValue(), r.postEquityValue(), r.postBondValue(), r.postCashValue(),
                r.postEquityPct(), r.postBondPct(), r.postCashPct(),
                r.equityTradeAmount(), r.bondTradeAmount(), r.cashTradeAmount());
    }
}
