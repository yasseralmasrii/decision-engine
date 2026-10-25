package com.yasser.decisionengine.engine;

import com.yasser.decisionengine.model.ProposedAction;
import com.yasser.decisionengine.model.Verdict;
import com.yasser.decisionengine.signals.DeploySignals;
import com.yasser.decisionengine.signals.RefundSignals;
import com.yasser.decisionengine.signals.TicketSignals;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DecisionServiceTest {
    private final DecisionService service = new DecisionService(
            List.of(new RefundSignals(), new DeploySignals(), new TicketSignals()),
            new PolicyEngine());
    private Verdict decide(String domain, String type, Map<String, Object> params) {
        return service.decide(new ProposedAction(domain, type, params, "agent-1")).verdict();
    }
    @Test
    void smallStoreCreditRefund_executes() {
        assertEquals(Verdict.EXECUTE, decide("refund", "issue_refund",
                Map.of("orderId", "A-1001", "amount", 30, "daysSincePurchase", 5, "method", "store_credit")));
    }
    @Test
    void refundWithoutOrderId_asks() {
        assertEquals(Verdict.ASK, decide("refund", "issue_refund",
                Map.of("amount", 30, "method", "store_credit")));
    }
    @Test
    void largeCardRefund_escalates() {
        assertEquals(Verdict.ESCALATE, decide("refund", "issue_refund",
                Map.of("orderId", "A-2002", "amount", 800, "daysSincePurchase", 3, "method", "card")));
    }
    @Test
    void prodDeployWithTestsRunning_defers() {
        assertEquals(Verdict.DEFER, decide("deploy", "deploy",
                Map.of("service", "api", "environment", "production", "rollbackAvailable", true)));
    }
    @Test
    void prodDeployFailedTestsNoRollback_refuses() {
        assertEquals(Verdict.REFUSE, decide("deploy", "deploy",
                Map.of("service", "api", "environment", "production",
                        "testsPassed", false, "rollbackAvailable", false)));
    }
    @Test
    void unknownDomain_refuses() {
        assertEquals(Verdict.REFUSE, decide("payroll", "pay", Map.of()));
    }
}