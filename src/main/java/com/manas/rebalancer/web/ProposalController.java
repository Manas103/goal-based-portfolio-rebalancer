package com.manas.rebalancer.web;

import com.manas.rebalancer.model.RebalanceProposalEntity;
import com.manas.rebalancer.repository.RebalanceProposalRepository;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

/**
 * Read-only API behind the Angular console. There is deliberately no write
 * endpoint here: the console explains proposals, it does not create,
 * approve or delete them (see {@code RebalanceProposalRepository}'s Javadoc
 * for why the ledger has no mutation path at all).
 */
@RestController
@RequestMapping("/api/proposals")
@CrossOrigin(origins = {"http://localhost:4200"})
public class ProposalController {

    private final RebalanceProposalRepository repository;

    public ProposalController(RebalanceProposalRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<ProposalResponse> all() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(RebalanceProposalEntity::getProposedAt).reversed())
                .map(ProposalResponse::from)
                .toList();
    }

    @GetMapping("/{accountId}")
    public List<ProposalResponse> byAccount(@PathVariable String accountId) {
        return repository.findByAccountIdOrderByProposedAtAsc(accountId).stream()
                .map(ProposalResponse::from)
                .toList();
    }
}
