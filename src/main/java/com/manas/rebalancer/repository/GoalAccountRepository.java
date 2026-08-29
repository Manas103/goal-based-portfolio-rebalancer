package com.manas.rebalancer.repository;

import com.manas.rebalancer.model.GoalAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Ordinary CRUD for account state, unlike {@link RebalanceProposalRepository}.
 * An account's own holdings are allowed to be updated (that is what a real
 * trade settling, or a market move, would do to it); it is the ledger of
 * proposals, not the account itself, that must never be mutated after the
 * fact.
 */
public interface GoalAccountRepository extends JpaRepository<GoalAccountEntity, String> {
}
