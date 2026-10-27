package com.yasser.decisionengine.demo;

import com.yasser.decisionengine.TestEngine;
import com.yasser.decisionengine.engine.DecisionService;
import com.yasser.decisionengine.model.Decision;
import com.yasser.decisionengine.model.ProposedAction;
import com.yasser.decisionengine.model.Verdict;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every curated demo scenario must produce the verdict it promises. */
class ScenarioTest {

    private final DecisionService service = TestEngine.decisions();

    @Test
    void everyScenarioProducesItsExpectedVerdict() {
        List<String> failures = new ArrayList<>();
        for (var s : Scenarios.ALL) {
            Decision d = service.preview(s.action());
            if (d.verdict() != s.expected()) {
                failures.add(s.id() + ": expected " + s.expected() + " but got " + d.verdict()
                        + " (rule " + d.ruleFired() + ", " + d.reasoning() + ")");
            }
        }
        assertTrue(failures.isEmpty(), String.join("\n", failures));
    }

    @Test
    void scenariosCoverEveryVerdict() {
        for (Verdict v : Verdict.values()) {
            assertTrue(Scenarios.ALL.stream().anyMatch(s -> s.expected() == v), "No scenario for " + v);
        }
    }

    @Test
    void agentCannotLieAboutRefundHistory() {
        // Old design trusted "refundsLast90Days" from the request. Now it is ignored:
        // customer C-5 really has 4 refunds, so confidence drops and a human decides.
        Decision d = service.decide(new ProposedAction("refund", "issue_refund",
                Map.of("orderId", "A-1006", "amount", 40, "refundsLast90Days", 0), "sneaky-agent"));
        assertEquals(Verdict.ESCALATE, d.verdict());
        assertTrue(d.signals().evidence().stream().anyMatch(e -> e.contains("frequent refunder")));
    }

    @Test
    void domainIsCaseInsensitive() {
        Decision d = service.decide(new ProposedAction("  Refund ", "ISSUE_REFUND",
                Map.of("orderId", "A-1001", "amount", 25), "agent"));
        assertEquals(Verdict.EXECUTE, d.verdict());
    }

    @Test
    void unknownOrderAsks() {
        Decision d = service.decide(new ProposedAction("refund", "issue_refund",
                Map.of("orderId", "A-9999", "amount", 25), "agent"));
        assertEquals(Verdict.ASK, d.verdict());
        assertTrue(d.nextStep().contains("A-9999"));
    }

    @Test
    void outsideRefundWindow_escalates() {
        Decision d = service.decide(new ProposedAction("refund", "issue_refund",
                Map.of("orderId", "A-1008", "amount", 20), "agent"));
        assertEquals(Verdict.ESCALATE, d.verdict());
        assertEquals("low-confidence", d.ruleFired());
    }

    @Test
    void commitBuiltForAnotherService_refuses() {
        Decision d = service.decide(new ProposedAction("deploy", "deploy",
                Map.of("service", "api", "environment", "production", "commit", "5d6e7f8"), "agent"));
        assertEquals(Verdict.REFUSE, d.verdict());
    }
}
