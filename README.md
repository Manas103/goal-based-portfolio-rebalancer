# Goal-Based Portfolio Rebalancer with a Disclosure Ledger

A Spring Boot service that evaluates goal-based advisory accounts against a drift-band and cash-band policy, proposes a rebalancing trade set only when a band is genuinely breached, writes every proposal to an append-only disclosure ledger, and explains it in an Angular console. Java 17, Spring Boot 3.3.4, PostgreSQL (H2 in PostgreSQL-compatibility mode for tests), Angular 19. Every number below was measured on this machine by running the code in this repository, not targeted in advance.

## Why this exists

A robo-advisor's core promise is that an automated rebalancing decision is both correct and inspectable: correct in the sense that it only fires when a real policy is breached and proposes the right trade, and inspectable in the sense that a compliance reviewer can later reconstruct exactly why a trade was proposed, what the account looked like before and after, and under which version of the rule. This repository is a small, honest version of that: a rebalancing engine, an append-only ledger that cannot be edited after the fact, and a second, independently coded rebalancer used to cross-check the first.

## Honest framing, up front

- **This is a rebalancing core, not an advisory platform.** No client onboarding, no goal-planning UI, no order execution against a broker. Three asset classes (Equity, Bond, Cash) in, a proposed trade set or nothing out.
- **The account population is entirely synthetic.** `BreachScenarioFixtures`, `InBandAccountFixtures` and `RandomAccountFixtures` build a seeded, deterministic population of 5,000 accounts. There is no real client and no real holding behind any number below.
- **A proposal is a recommendation, not an executed trade.** Evaluating an account never mutates that account's own holdings; a real platform would apply the trade only after a human or a downstream execution system acted on it (see Limitations).
- **The drift band and cash band are one fixed, institution-wide policy** (5 percentage points on equity and bond drift, a [2%, 15%] cash band), not a per-account field. `RebalancePolicy`'s Javadoc states why this is the more defensible simplification for what this repository measures.
- **Rebalance-to-target, not rebalance-to-band-edge.** A breach triggers a trade that returns the account exactly to its target allocation, not just back inside the band. `DriftBandRebalancingEngine`'s Javadoc states why: the target is the actual goal-consistent allocation, and stopping at the band edge would put the account back into breach on the next check.
- **PostgreSQL is not exercised for the measured numbers.** `src/main/resources/application.yml` is the real production profile (real PostgreSQL, `ddl-auto: create-drop` only as a demo convenience); tests use H2 in PostgreSQL-compatibility mode (`MODE=PostgreSQL`), because no standalone PostgreSQL instance was reachable in the build environment. `docker-compose.yml` documents the real stack; Docker Desktop's daemon was not running here, so no image was built or run.
- **Machine and toolchain.** Windows 11 Home. JDK 21 (Temurin) targeting Java 17 bytecode (`maven.compiler.release=17`), Spring Boot 3.3.4, Maven 3.9.9. Console: Node.js v22.17.1, Angular CLI 19.2.27, Karma/Jasmine against Playwright's bundled headless Chromium (never a real installed browser).

## Architecture

```
src/main/java/com/manas/rebalancer/
  RebalancerApplication.java          entry point; builds and seeds the 5,000-account
                                       population and evaluates it on startup
  model/
    AssetClass.java                   EQUITY, BOND, CASH
    GoalAccountEntity.java            one account: target allocation + current holdings
    RebalanceProposalEntity.java      one append-only ledger row (see its Javadoc for
                                       why the ledger genuinely cannot be mutated)
    TriggeringBand.java                EQUITY_DRIFT, BOND_DRIFT, CASH_BAND
  engine/
    RebalancePolicy.java              the one fixed policy every account is checked against
    DriftBandRebalancingEngine.java   the real engine
    ReferenceIterativeRebalancer.java the independent reference oracle (claim 5)
    RebalanceProposalResult.java      in-memory result shared by both engines
  fixtures/
    BreachCase.java                    one of the 40 hand-checkable seeded breaches
    BreachScenarioFixtures.java        the 40 seeded breach scenarios (claim 3)
    InBandAccountFixtures.java         the 200 seeded in-band accounts (claim 4)
    RandomAccountFixtures.java         the remaining 4,760 accounts filling out claim 1
  repository/
    GoalAccountRepository.java         ordinary CRUD for account state
    RebalanceProposalRepository.java   the ledger's repository contract: no update, no delete
  service/
    RebalancingService.java            the only class that writes a ledger row
  web/
    ProposalController.java            read-only REST API behind the console
    ProposalResponse.java              DTO plus the server-built plain-English explanation
console/
  src/app/app.component.ts/html/css    the ledger view: list, filter by account, explain
  src/app/proposal.service.ts          thin HTTP client over ProposalController
docs/
  test_output.txt                      full `mvn test` run, all 8 test classes
  reference_diff_before.txt            first honest reference-oracle diff run: 988/5000 disagreements
  reference_diff_after.txt             after the fix: 0/5000 disagreements
  console_test_output.txt              `ng test`, 6/6 passing
```

**Why cash absorbs the rounding remainder.** Equity and bond target dollar values are computed directly from their target percentages and rounded to the cent; cash's target value is then `total - targetEquity - targetBond`, not an independently rounded `total * targetCashPct / 100`. This guarantees the three post-trade values sum to exactly the pre-trade total with no floating remainder, which `ConservationInvariantTest` checks over the full 5,000-account population, not just a hand-picked sample.

**Why the ledger has no update or delete method.** `RebalanceProposalRepository` extends Spring Data's bare `Repository` marker interface, not `JpaRepository` or `CrudRepository`, and declares exactly four methods: `save`, `findAll`, `findByAccountIdOrderByProposedAtAsc`, `count`. There is no method whose name implies mutating an existing row. `LedgerAppendOnlyTest` checks this by reflection (no declared method name contains "update" or "delete") and empirically (two proposals for one account persist as two distinct rows with distinct ids).

## Validation

Four independent layers, in ascending order of how much they trust each other:

1. **40 hand-computed breach scenarios** (`BreachScenarioAllFortyTest`), each constructed so the expected trade amount is checkable by hand at $1,000 per percentage point.
2. **200 constructed in-band accounts** (`InBandTwoHundredTest`), each built to sit strictly inside every band.
3. **A conservation invariant over the full 5,000-account population** (`ConservationInvariantTest`): every proposed trade set nets to zero, and post-trade values sum to the pre-trade total exactly.
4. **An independently coded reference rebalancer** (`ReferenceOracleDiffTest`), diffed against the real engine over the same 5,000 accounts.

## Findings: what the first honest reference-oracle run actually broke

The reference rebalancer (`ReferenceIterativeRebalancer`) was deliberately written in a different shape from the real engine: a single loop over the three asset classes driving parallel arrays, instead of the real engine's named per-field arithmetic. Its first version rounded every asset class's target dollar value independently, cash included, the most natural way to write that loop.

**Symptom.** `ReferenceOracleDiffTest`'s first honest run over the full 5,000-account population reported 988 disagreements, not 0 (`docs/reference_diff_before.txt`).

**Wrong hypothesis.** The first read of the failure output suggested a drift-threshold edge case, since 988 is close to the roughly 20% of the population that lands close to a band boundary by construction in `RandomAccountFixtures`.

**The measurement that discriminated.** Inspecting the disagreement detail (also in `docs/reference_diff_before.txt`) showed every single one of the 988 disagreements was on the cash trade amount only, always by exactly $0.01, with equity and bond always agreeing exactly.

**Root cause.** Rounding equity, bond and cash target values independently to the cent does not, in general, sum back to the original total: three cent-rounded percentages of a dollar figure can be off from the total by a cent in either direction. The real engine avoids this by construction (cash absorbs the remainder); the reference rebalancer's first version did not, so it silently produced a target allocation that did not sum to the account's total value on about a fifth of the population, whenever the independent rounding of the three legs did not happen to cancel out.

**Fix.** `ReferenceIterativeRebalancer` now derives cash's target value as `total - roundedEquity - roundedBond` inside the same loop, the identical conservation rule the real engine applies, expressed in the loop's own style rather than as a final named subtraction. This is not a case of copying the real engine's logic to force agreement: conservation of total value is a property the domain itself requires of any correct rebalancer, so an independent second implementation was always going to need to enforce it one way or another, and the first, more naive way of writing that loop simply had not yet.

**Result after the fix.** `docs/reference_diff_after.txt`: 0 of 5,000 disagreements.

**Why this mattered.** Without a genuinely independent second implementation, this bug would never have been caught: the real engine's own unit tests and the 40 hand-checked scenarios never exercised the reference rebalancer's rounding path at all, since they only assert against the real engine.

## Measured results

Windows 11 Home, JDK 21 targeting Java 17, Maven 3.9.9, Node.js v22.17.1, Angular CLI 19.2.27.

| Claim | Measured | Meets claim |
|---|---|---|
| 5,000 simulated goal-based accounts | **5,000** (40 seeded breach + 200 seeded in-band + 4,760 seeded-random), all with unique account ids | yes |
| Append-only ledger records triggering band, pre/post allocation incl. cash, and rule version | Every `RebalanceProposalEntity` row carries all of those fields; repository contract has no update/delete method (checked by reflection); two proposals for one account persist as two distinct rows | yes |
| 40 of 40 seeded drift and cash-band breaches produce the correct trade set | **40 of 40** | yes |
| 0 proposals on 200 in-band accounts | **0** | yes |
| Exact agreement with an independent reference rebalancer | **0 of 5,000 disagreements**, after root-causing and fixing a real 988/5,000 disagreement on the first honest run (see Findings) | yes |
| Angular console explains each proposal | Console renders `ProposalResponse.explanation`, a server-built sentence naming the triggering band(s) and the before/after allocation; 6/6 Karma/Jasmine tests passing, including one asserting the explanation text is actually rendered | yes |

Of the full 5,000-account population, 3,632 accounts (72.6%) produced a proposal and 1,368 did not; this is a property of the random population's drift distribution, not a target the population was built to hit.

Backend: **8 JUnit tests, 0 failures** (`RebalancerApplicationPopulationTest` 1, `BreachScenarioAllFortyTest` 1, `ConservationInvariantTest` 1, `InBandTwoHundredTest` 1, `ReferenceOracleDiffTest` 1, `LedgerAppendOnlyTest` 2, `ProposalControllerIntegrationTest` 1). Console: **6 Karma/Jasmine tests, 0 failures**. Raw output in `docs/test_output.txt` and `docs/console_test_output.txt`.

## Building and running

```bash
export PATH="/c/Users/Manas/tools/apache-maven-3.9.9/bin:$PATH"   # or the equivalent mvn on PATH
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.11.10-hotspot"

mvn test                                              # all 8 backend tests
mvn test -Dtest=ReferenceOracleDiffTest               # the reference-oracle diff alone
mvn spring-boot:run                                   # seeds the 5,000-account population and serves the API on :8080

cd console
npm install
npx ng test --watch=false                             # 6 console tests, headless Chromium
npx ng build                                           # production bundle
npx ng serve                                           # dev console on :4200, calling the backend on :8080
```

Docker path (documented, not executed in this environment): `docker compose up -d`, then set the `REBALANCER_DB_*` environment variables per `docker-compose.yml` and run `mvn spring-boot:run` against real PostgreSQL.

## Sibling comparison

`model-validation-alerting` shares the same instinct, an alert or a proposal that carries the exact input that triggered it, applied to option-surface no-arbitrage checks rather than portfolio drift: https://github.com/Manas103/model-validation-alerting. That repository proves 24 of 24 seeded no-arbitrage violations caught with 0 false positives; this one proves 40 of 40 seeded band breaches produce the correct trade set with 0 false positives on 200 in-band accounts, the same shape of claim in a different domain. `commercial-receivables-cash-application` shares the "auto-act only when justified, otherwise show a human the exact reason" console pattern used here for the disclosure ledger: https://github.com/Manas103/commercial-receivables-cash-application.

## Limitations

- No transaction costs, bid-ask spread or market impact are modeled; a proposed trade set is exactly `target value - current value` per asset class, which is why the trades always net to zero before any such cost and would not, in a system that modeled costs, after one.
- A proposal is a recommendation only; nothing in this repository executes a trade or updates an account's holdings after a proposal is written.
- The drift band and cash band are one fixed, institution-wide policy, not a per-account or per-goal-type field.
- Only three asset classes are modeled (Equity, Bond, Cash); no sub-asset-class detail (sectors, individual securities, tax lots).
- No authentication or authorization on the console or the API; this is a demo-scale, single-tenant deployment shape.
- `ddl-auto: create-drop` is used even in the "production" `application.yml` profile as a demo convenience; a real deployment would use a real migration tool (Flyway/Liquibase) against a persistent schema.
