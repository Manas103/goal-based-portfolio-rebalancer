package com.manas.rebalancer;

import com.manas.rebalancer.fixtures.BreachCase;
import com.manas.rebalancer.fixtures.BreachScenarioFixtures;
import com.manas.rebalancer.fixtures.InBandAccountFixtures;
import com.manas.rebalancer.fixtures.RandomAccountFixtures;
import com.manas.rebalancer.model.GoalAccountEntity;
import com.manas.rebalancer.model.RebalanceProposalEntity;
import com.manas.rebalancer.repository.GoalAccountRepository;
import com.manas.rebalancer.service.RebalancingService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

/**
 * On startup (real database or {@code mvn spring-boot:run} against the H2
 * fallback, never during {@code mvn test}, see {@code rebalancer.seed-on-startup}
 * in the two {@code application.yml} profiles), seeds the full 5,000-account
 * population behind claim 1: the 40 hand-checkable breach scenarios, the 200
 * constructed in-band accounts, and 4,760 seeded-random accounts, exactly
 * 5,000 in total, then runs every one of them through the real engine and
 * writes every resulting proposal to the ledger.
 */
@SpringBootApplication
public class RebalancerApplication {

    public static final int TOTAL_POPULATION = 5000;

    public static void main(String[] args) {
        SpringApplication.run(RebalancerApplication.class, args);
    }

    public static List<GoalAccountEntity> buildFullPopulation() {
        List<GoalAccountEntity> population = new ArrayList<>(TOTAL_POPULATION);
        for (BreachCase breachCase : BreachScenarioFixtures.ALL) {
            population.add(breachCase.toAccount());
        }
        population.addAll(InBandAccountFixtures.generate(200));
        int remaining = TOTAL_POPULATION - population.size();
        population.addAll(RandomAccountFixtures.generate(remaining, 42L));
        return population;
    }

    @Bean
    @ConditionalOnProperty(prefix = "rebalancer", name = "seed-on-startup", havingValue = "true")
    CommandLineRunner seedAndEvaluatePopulation(GoalAccountRepository accountRepository, RebalancingService rebalancingService) {
        return args -> {
            List<GoalAccountEntity> population = buildFullPopulation();
            accountRepository.saveAll(population);
            List<RebalanceProposalEntity> proposals = rebalancingService.evaluateAndRecordAll(population);
            System.out.println("seeded " + population.size() + " goal-based accounts, wrote "
                    + proposals.size() + " proposals to the disclosure ledger");
        };
    }
}
