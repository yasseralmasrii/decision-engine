package com.yasser.decisionengine.model;

import java.time.Instant;

/**
 * One permanent, tamper-evident record per decision.
 * Each entry stores the previous entry's hash, so editing any past
 * entry breaks the chain (see AuditLog.verify()).
 *
 * @param proposal null when the action was submitted directly, not by an AI
 *                 agent
 */
public record AuditEntry(
                String id,
                Instant timestamp,
                ProposedAction input,
                Decision decision,
                AgentProposal proposal,
                String prevHash,
                String hash) {
}
