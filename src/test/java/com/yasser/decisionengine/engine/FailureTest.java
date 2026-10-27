package com.yasser.decisionengine.engine;

import com.yasser.decisionengine.TestEngine;
import com.yasser.decisionengine.ledger.RefundLedger;
import com.yasser.decisionengine.model.Decision;
import com.yasser.decisionengine.model.ProposedAction;
import com.yasser.decisionengine.model.Verdict;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The deliberate failure test (see docs/failure-test.md).
 *
 * Version 1 of the engine judged every request in isolation. These attacks
 * broke it:
 * - Replay: the same $25 refund on a $45 order, filed 10 times, was approved 10
 * times.
 * - Salami: a large refund split into small ones slipped under every threshold.
 * The refund ledger and the velocity limit are the fix; these tests keep it
 * fixed.
 */
class FailureTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-01T10:00:00Z"), ZoneOffset.UTC);
    private final RefundLedger ledger = new RefundLedger();
    private final DecisionService engine = TestEngine.decisions(ledger, clock);

    private static ProposedAction refund(String orderId, double amount) {
        return new ProposedAction("refund", "issue_refund", Map.of("orderId", orderId, "amount", amount), "attacker");
    }

    @Test
    void replayAttack_sameRefundTenTimes_onlyFirstExecutes() {
        List<Verdict> verdicts = new ArrayList<>();
        for (int i = 0; i < 10; i++)
            verdicts.add(engine.decide(refund("A-1001", 25)).verdict());

        assertEquals(Verdict.EXECUTE, verdicts.get(0));
        assertTrue(verdicts.subList(1, 10).stream().allMatch(v -> v == Verdict.REFUSE), verdicts.toString());
        assertEquals(25.0, ledger.approvedForOrder("A-1001"), 0.001); // not $250 on a $45 order
    }

    @Test
    void replayIsRefusedWithAnExplanation() {
        engine.decide(refund("A-1001", 25));
        Decision second = engine.decide(refund("A-1001", 25));
        assertEquals("policy-violation", second.ruleFired());
        assertTrue(second.nextStep().contains("refundable balance $20.00"), second.nextStep());
        assertTrue(second.signals().evidence().stream().anyMatch(e -> e.contains("already approved")));
    }

    @Test
    void salamiAttack_smallRefundsAcrossOrders_escalateAtTheDailyLimit() {
        // Customer C-7 has four $60 orders. Four $50 refunds = $200 against a $150
        // daily automatic limit.
        List<Verdict> verdicts = List.of(
                engine.decide(refund("A-1010", 50)).verdict(),
                engine.decide(refund("A-1011", 50)).verdict(),
                engine.decide(refund("A-1012", 50)).verdict(),
                engine.decide(refund("A-1013", 50)).verdict());
        assertEquals(List.of(Verdict.EXECUTE, Verdict.EXECUTE, Verdict.EXECUTE, Verdict.ESCALATE), verdicts);
    }

    @Test
    void previewNeverSpendsBudget() {
        for (int i = 0; i < 5; i++) {
            assertEquals(Verdict.EXECUTE, engine.preview(refund("A-1001", 25)).verdict());
        }
        assertEquals(0.0, ledger.approvedForOrder("A-1001"), 0.001);
    }

    @Test
    void onlyExecutedRefundsAreRemembered() {
        engine.decide(refund("A-1003", 1200)); // escalated, not executed
        assertEquals(0.0, ledger.approvedForOrder("A-1003"), 0.001);
    }

    /**
     * Known limit, kept honest: the ledger is in memory, so a restart forgets past
     * approvals.
     */
    @Test
    void knownLimit_freshLedgerForgetsPastApprovals() {
        engine.decide(refund("A-1001", 25));
        DecisionService restarted = TestEngine.decisions(new RefundLedger(), clock);
        assertEquals(Verdict.EXECUTE, restarted.decide(refund("A-1001", 25)).verdict());
    }
}
