package com.yasser.decisionengine.engine;

import com.yasser.decisionengine.model.Decision;
import com.yasser.decisionengine.model.RuleCheck;
import com.yasser.decisionengine.model.Verdict;
import com.yasser.decisionengine.signals.SignalsBuilder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyEngineTest {

    private final PolicyEngine engine = new PolicyEngine();

    private SignalsBuilder safe() {
        return new SignalsBuilder().confidence(0.9).risk(0.1).reversible(true).costOfErrorUsd(20);
    }

    @Test
    void violationRefuses_evenWhenFullyConfident() {
        Decision d = engine.decide(safe().confidence(1.0).violation("Change freeze").build());
        assertEquals(Verdict.REFUSE, d.verdict());
        assertTrue(d.nextStep().contains("Change freeze"));
    }

    @Test
    void missingInfoAsks_andNamesWhatIsMissing() {
        Decision d = engine.decide(safe().missing("orderId").build());
        assertEquals(Verdict.ASK, d.verdict());
        assertEquals("Ask the requester for: orderId.", d.nextStep());
    }

    @Test
    void pendingEvidenceDefers() {
        Decision d = engine.decide(safe().pending("CI to finish").build());
        assertEquals(Verdict.DEFER, d.verdict());
    }

    @Test
    void lowConfidenceEscalates() {
        assertEquals(Verdict.ESCALATE, engine.decide(safe().confidence(0.5).build()).verdict());
    }

    /**
     * Regression test for the old bug: huge irreversible actions were REFUSED
     * instead of escalated.
     */
    @Test
    void maxRiskIrreversible_escalatesNotRefuses() {
        Decision d = engine.decide(safe().risk(1.0).reversible(false).costOfErrorUsd(1200)
                .approver("finance-approver").build());
        assertEquals(Verdict.ESCALATE, d.verdict());
        assertEquals("Route to finance-approver for approval.", d.nextStep());
    }

    @Test
    void smallIrreversibleAction_canStillExecute() {
        assertEquals(Verdict.EXECUTE,
                engine.decide(safe().reversible(false).costOfErrorUsd(25).build()).verdict());
    }

    @Test
    void irreversibleAboveLimit_escalates() {
        assertEquals(Verdict.ESCALATE,
                engine.decide(safe().reversible(false).costOfErrorUsd(150).build()).verdict());
    }

    @Test
    void safeActionExecutes() {
        assertEquals(Verdict.EXECUTE, engine.decide(safe().build()).verdict());
    }

    @Test
    void traceEvaluatesEveryRule_andExactlyOneDecides() {
        Decision d = engine.decide(safe().missing("x").pending("y").build());
        assertEquals(engine.rules().size(), d.trace().size());
        assertEquals(1, d.trace().stream().filter(RuleCheck::decided).count());
        assertEquals("missing-info", d.ruleFired());
        assertTrue(
                d.trace().stream().anyMatch(c -> c.rule().equals("pending-evidence") && c.matched() && !c.decided()));
    }

    @Test
    void decisionCarriesPolicyVersion() {
        assertEquals(PolicyEngine.POLICY_VERSION, engine.decide(safe().build()).policyVersion());
    }
}
