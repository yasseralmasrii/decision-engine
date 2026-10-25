package com.yasser.decisionengine.audit;

import com.yasser.decisionengine.model.AuditEntry;
import com.yasser.decisionengine.model.Decision;
import com.yasser.decisionengine.model.ProposedAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Keeps a record of every decision: inputs, signals, rule fired, reasoning, outcome.
 * In-memory on purpose (see README: "out of scope"). It resets when the app restarts.
 */
@Component
public class AuditLog {
    private static final Logger log = LoggerFactory.getLogger(AuditLog.class);
    private static final int MAX_ENTRIES = 500;
    private final Deque<AuditEntry> entries = new ConcurrentLinkedDeque<>();
    public AuditEntry record(ProposedAction input, Decision decision) {
        AuditEntry entry = new AuditEntry(
                UUID.randomUUID().toString(), Instant.now(), input, decision);
        entries.addFirst(entry);                        
        while (entries.size() > MAX_ENTRIES) entries.pollLast();
        log.info("DECISION {} | {}.{} -> {} (rule: {})",
                entry.id(), input.domain(), input.type(),
                decision.verdict(), decision.ruleFired());
        return entry;
    }
    public List<AuditEntry> recent(int limit) {
        return entries.stream().limit(limit).toList();
    }
    public Optional<AuditEntry> find(String id) {
        return entries.stream().filter(e -> e.id().equals(id)).findFirst();
    }
}