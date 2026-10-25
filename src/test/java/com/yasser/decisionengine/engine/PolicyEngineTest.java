package com.yasser.decisionengine.engine;

import com.yasser.decisionengine.model.ProposedAction;
import com.yasser.decisionengine.model.Signals;
import com.yasser.decisionengine.model.Verdict;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PolicyEngineTest {
    private final PolicyEngine engine = new PolicyEngine();
    private final ProposedAction action =
            new ProposedAction("refund", "issue_refund", Map.of(), "agent-1");

    private Signals signals(double confidence, double risk, boolean reversible,
                            List<String> missing, double cost) {
        return new Signals(confidence, risk, reversible, List.of(), missing, cost);
    }
    @Test
    void refusesCriticalIrreversible_evenWhenConfident() {
        var d = engine.decide(action, signals(0.99, 0.95, false, List.of(), 0));
        assertEquals(Verdict.REFUSE, d.verdict());
    }
    @Test
    void asksWhenInfoIsMissing() {
        var d = engine.decide(action, signals(0.9, 0.1, true, List.of("orderId"), 10));
        assertEquals(Verdict.ASK, d.verdict());
    }
    @Test
    void defersOnWeakEvidence() {
        var d = engine.decide(action, signals(0.3, 0.1, true, List.of(), 10));
        assertEquals(Verdict.DEFER, d.verdict());
    }
    @Test
    void escalatesExpensiveActions() {
        var d = engine.decide(action, signals(0.95, 0.2, true, List.of(), 2000));
        assertEquals(Verdict.ESCALATE, d.verdict());
    }
    @Test
    void executesSafeActions() {
        var d = engine.decide(action, signals(0.9, 0.1, true, List.of(), 20));
        assertEquals(Verdict.EXECUTE, d.verdict());
    }
}