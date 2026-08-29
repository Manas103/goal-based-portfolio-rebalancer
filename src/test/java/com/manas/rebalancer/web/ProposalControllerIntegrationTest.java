package com.manas.rebalancer.web;

import com.manas.rebalancer.model.GoalAccountEntity;
import com.manas.rebalancer.model.RebalanceProposalEntity;
import com.manas.rebalancer.repository.GoalAccountRepository;
import com.manas.rebalancer.service.RebalancingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end wiring proof: a breaching account persisted through
 * {@link GoalAccountRepository}, evaluated by {@link RebalancingService},
 * written to the ledger, and served back out over the real REST API the
 * Angular console calls, with a non-empty human-readable explanation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProposalControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private GoalAccountRepository accountRepository;

    @Autowired
    private RebalancingService rebalancingService;

    @Test
    void breachingAccountIsWrittenAndServedWithAnExplanation() {
        GoalAccountEntity breaching = new GoalAccountEntity(
                "CONTROLLER-IT-1", "P1",
                new BigDecimal("60"), new BigDecimal("32"), new BigDecimal("8"),
                new BigDecimal("66000.00"), new BigDecimal("32000.00"), new BigDecimal("2000.00"));
        accountRepository.save(breaching);
        List<RebalanceProposalEntity> written = rebalancingService.evaluateAndRecordAll(List.of(breaching));
        assertEquals(1, written.size());

        ResponseEntity<List<ProposalResponse>> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/proposals/CONTROLLER-IT-1",
                org.springframework.http.HttpMethod.GET, null,
                new ParameterizedTypeReference<List<ProposalResponse>>() {
                });

        assertEquals(200, response.getStatusCode().value());
        List<ProposalResponse> body = response.getBody();
        assertEquals(1, body.size());
        ProposalResponse proposal = body.get(0);
        assertEquals("CONTROLLER-IT-1", proposal.accountId());
        assertEquals("EQUITY_DRIFT", proposal.triggeringBands());
        assertFalse(proposal.explanation().isBlank());
        assertTrue(proposal.explanation().toLowerCase().contains("equity drift"));
    }
}
