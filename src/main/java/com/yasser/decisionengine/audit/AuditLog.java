package com.yasser.decisionengine.audit;

import com.yasser.decisionengine.model.AgentProposal;
import com.yasser.decisionengine.model.AuditEntry;
import com.yasser.decisionengine.model.Decision;
import com.yasser.decisionengine.model.ProposedAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Append-only, hash-chained record of every decision.
 * Each entry's hash covers its content AND the previous entry's hash,
 * so editing any past entry is detectable with verify().
 *
 * Intentionally out of scope: persistence. The log lives in memory and
 * resets on restart; DemoSeeder re-creates sample decisions at startup.
 */
@Component
public class AuditLog {

    static final String GENESIS = "0".repeat(64);
    static final int MAX_ENTRIES = 1000;

    private static final Logger log = LoggerFactory.getLogger(AuditLog.class);

    private final Clock clock;
    private final Deque<AuditEntry> entries = new ArrayDeque<>(); // newest first
    private String lastHash = GENESIS;

    public AuditLog(Clock clock) {
        this.clock = clock;
    }

    public AuditEntry record(ProposedAction input, Decision decision) {
        return record(input, decision, null);
    }

    public synchronized AuditEntry record(ProposedAction input, Decision decision, AgentProposal proposal) {
        String id = UUID.randomUUID().toString();
        Instant timestamp = Instant.now(clock);
        String hash = hash(lastHash, id, timestamp, input, decision, proposal);
        AuditEntry entry = new AuditEntry(id, timestamp, input, decision, proposal, lastHash, hash);

        entries.addFirst(entry);
        lastHash = hash;
        while (entries.size() > MAX_ENTRIES)
            entries.pollLast();

        log.info("DECISION {} | {}.{} -> {} (rule: {})", id, input.domain(), input.type(),
                decision.verdict(), decision.ruleFired());
        return entry;
    }

    public synchronized List<AuditEntry> recent(int limit) {
        int safe = Math.max(0, Math.min(limit, MAX_ENTRIES));
        return entries.stream().limit(safe).toList();
    }

    public synchronized Optional<AuditEntry> find(String id) {
        return entries.stream().filter(e -> e.id().equals(id)).findFirst();
    }

    public synchronized ChainStatus verify() {
        return verifyChain(new ArrayList<>(entries));
    }

    /**
     * Verifies a newest-first list of entries. Package-visible so tests can tamper
     * with a copy.
     */
    static ChainStatus verifyChain(List<AuditEntry> newestFirst) {
        AuditEntry older = null;
        for (int i = newestFirst.size() - 1; i >= 0; i--) { // walk oldest -> newest
            AuditEntry e = newestFirst.get(i);
            String expected = hash(e.prevHash(), e.id(), e.timestamp(), e.input(), e.decision(), e.proposal());
            if (!expected.equals(e.hash())) {
                return new ChainStatus(false, newestFirst.size(), e.id(), "Entry content was modified");
            }
            if (older != null && !e.prevHash().equals(older.hash())) {
                return new ChainStatus(false, newestFirst.size(), e.id(),
                        "Chain link broken (entry removed or reordered)");
            }
            older = e;
        }
        return new ChainStatus(true, newestFirst.size(), null, "All entries intact");
    }

    /**
     * Canonical form = the records' toString(), which is deterministic within one
     * running
     * process. If the log were persisted, this should become canonical JSON (RFC
     * 8785).
     */
    static String hash(String prevHash, String id, Instant timestamp, ProposedAction input,
            Decision decision, AgentProposal proposal) {
        String canonical = String.join("\n", prevHash, id, String.valueOf(timestamp),
                String.valueOf(input), String.valueOf(decision), String.valueOf(proposal));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every JVM", e);
        }
    }
}
