package com.manas.rebalancer.repository;

import com.manas.rebalancer.model.RebalanceProposalEntity;
import org.springframework.data.repository.Repository;

import java.util.List;

/**
 * The disclosure ledger's repository contract, and the reason the ledger is
 * genuinely append-only rather than append-only "by convention". This
 * interface extends Spring Data's bare {@link Repository} marker (which
 * declares zero methods of its own), not {@code JpaRepository} or
 * {@code CrudRepository}, and lists only the four methods below. There is no
 * {@code delete}, {@code deleteById}, {@code deleteAll} or any method whose
 * name implies an update-by-id anywhere in this file; {@code save} on an
 * entity with a null id (every {@code RebalanceProposalEntity} is
 * constructed that way, see its Javadoc) is always an INSERT under
 * {@code GenerationType.IDENTITY}, never an update, because there is no id
 * yet for Hibernate to match an existing row against.
 *
 * <p>{@code LedgerAppendOnlyTest} asserts this by reflection (no declared
 * method name contains "delete" or "update") and empirically (writing two
 * proposals for the same account and confirming both persist as two
 * distinct rows with distinct ids).
 */
public interface RebalanceProposalRepository extends Repository<RebalanceProposalEntity, Long> {

    RebalanceProposalEntity save(RebalanceProposalEntity entity);

    List<RebalanceProposalEntity> findAll();

    List<RebalanceProposalEntity> findByAccountIdOrderByProposedAtAsc(String accountId);

    long count();
}
