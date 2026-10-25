package com.yasser.decisionengine.audit;

import com.yasser.decisionengine.model.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AuditLogTest {
    @Test
    void recordsAndFindsDecisions() {
        AuditLog log = new AuditLog();
        var action = new ProposedAction("refund", "issue_refund", Map.of("amount", 30), "agent-1");
        var signals = new Signals(0.9, 0.1, true, List.of("Within 30-day window"), List.of(), 30);
        var decision = new Decision(Verdict.EXECUTE, signals, "safe-to-act", "Low risk.");
        AuditEntry entry = log.record(action, decision);
        assertNotNull(entry.id());
        assertNotNull(entry.timestamp());
        assertEquals(entry, log.find(entry.id()).orElseThrow());
        assertEquals(1, log.recent(10).size());
    }
}