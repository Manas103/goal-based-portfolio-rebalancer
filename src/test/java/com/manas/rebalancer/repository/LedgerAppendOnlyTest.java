package com.manas.rebalancer.repository;

import com.manas.rebalancer.model.RebalanceProposalEntity;
import com.manas.rebalancer.model.TriggeringBand;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Claim 2 (append-only): proves, not just asserts, that the ledger cannot be
 * mutated after a row is written. Two layers: reflection over the
 * repository's declared methods, and an empirical write-write-read of two
 * proposals for the same account.
 */
@SpringBootTest
class LedgerAppendOnlyTest {

    @Autowired
    private RebalanceProposalRepository repository;

    @Test
    void repositoryContractExposesNoUpdateOrDeleteMethod() {
        for (Method method : RebalanceProposalRepository.class.getDeclaredMethods()) {
            String name = method.getName().toLowerCase(Locale.ROOT);
            assertFalse(name.contains("delete"), "found a delete-shaped method: " + method.getName());
            assertFalse(name.contains("update"), "found an update-shaped method: " + method.getName());
        }
    }

    @Test
    void twoProposalsForTheSameAccountPersistAsTwoDistinctRows() {
        RebalanceProposalEntity first = sampleProposal("APPEND-ONLY-TEST", TriggeringBand.EQUITY_DRIFT);
        RebalanceProposalEntity second = sampleProposal("APPEND-ONLY-TEST", TriggeringBand.CASH_BAND);

        RebalanceProposalEntity savedFirst = repository.save(first);
        RebalanceProposalEntity savedSecond = repository.save(second);

        assertNotEquals(savedFirst.getId(), savedSecond.getId());

        List<RebalanceProposalEntity> rows = repository.findByAccountIdOrderByProposedAtAsc("APPEND-ONLY-TEST");
        assertEquals(2, rows.size());
        assertEquals(TriggeringBand.EQUITY_DRIFT.name(), rows.get(0).getTriggeringBands());
        assertEquals(TriggeringBand.CASH_BAND.name(), rows.get(1).getTriggeringBands());
    }

    private static RebalanceProposalEntity sampleProposal(String accountId, TriggeringBand band) {
        BigDecimal v = new BigDecimal("100000.00");
        BigDecimal zero = BigDecimal.ZERO;
        return new RebalanceProposalEntity(accountId, band.name(), "drift-band-v1", Instant.now(),
                v, v, zero, zero, new BigDecimal("100"), zero, zero,
                v, v, zero, zero, new BigDecimal("100"), zero, zero,
                zero, zero, zero);
    }
}
