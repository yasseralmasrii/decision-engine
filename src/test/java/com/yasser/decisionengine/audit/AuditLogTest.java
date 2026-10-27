package com.yasser.decisionengine.audit;

import com.yasser.decisionengine.engine.PolicyEngine;
import com.yasser.decisionengine.model.AuditEntry;
import com.yasser.decisionengine.model.Decision;
import com.yasser.decisionengine.model.ProposedAction;
import com.yasser.decisionengine.model.Verdict;
import com.yasser.decisionengine.signals.SignalsBuilder;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditLogTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-01T10:00:00Z"), ZoneOffset.UTC);
    private final AuditLog log = new AuditLog(clock);
    private final PolicyEngine engine = new PolicyEngine();

    private AuditEntry record(double amount) {
        var action = new ProposedAction("refund", "issue_refund", Map.of("amount", amount), "agent-1");
        Decision d = engine.decide(new SignalsBuilder().confidence(0.9).costOfErrorUsd(amount).build());
        return log.record(action, d);
    }

    @Test
    void entriesAreChainedAndTimestamped() {
        AuditEntry first = record(10);
        AuditEntry second = record(20);
        assertEquals(AuditLog.GENESIS, first.prevHash());
        assertEquals(first.hash(), second.prevHash());
        assertEquals(Instant.parse("2026-11-01T10:00:00Z"), first.timestamp());
        assertEquals(second, log.recent(1).get(0)); // newest first
        assertEquals(first, log.find(first.id()).orElseThrow());
    }

    @Test
    void intactChainVerifies() {
        record(10);
        record(20);
        record(30);
        ChainStatus status = log.verify();
        assertTrue(status.valid());
        assertEquals(3, status.entriesChecked());
    }

    @Test
    void editedEntryIsDetected() {
        record(10);
        AuditEntry target = record(20);
        record(30);
        List<AuditEntry> copy = new ArrayList<>(log.recent(10));

        // Someone rewrites history: the refused decision now says EXECUTE.
        Decision forged = new Decision(Verdict.EXECUTE, "safe-to-act", "forged", "forged",
                target.decision().signals(), target.decision().trace(), target.decision().policyVersion());
        int i = copy.indexOf(target);
        copy.set(i, new AuditEntry(target.id(), target.timestamp(), target.input(), forged,
                target.proposal(), target.prevHash(), target.hash()));

        ChainStatus status = AuditLog.verifyChain(copy);
        assertFalse(status.valid());
        assertEquals(target.id(), status.brokenAt());
    }

    @Test
    void deletedEntryIsDetected() {
        record(10);
        AuditEntry middle = record(20);
        record(30);
        List<AuditEntry> copy = new ArrayList<>(log.recent(10));
        copy.remove(middle);
        assertFalse(AuditLog.verifyChain(copy).valid());
    }

    @Test
    void limitIsClampedSafely() {
        record(10);
        assertEquals(0, log.recent(-5).size()); // used to throw IllegalArgumentException
        assertEquals(1, log.recent(1_000_000).size());
    }
}
