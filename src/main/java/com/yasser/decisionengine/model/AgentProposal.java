package com.yasser.decisionengine.model;

/**
 * Where a proposed action came from, when an AI agent proposed it.
 * Recorded for the audit trail. The engine NEVER uses selfReportedConfidence:
 * an agent grading its own homework is not evidence.
 */
public record AgentProposal(
                String proposer,
                String rationale,
                double selfReportedConfidence,
                String sourceTicketId) {
}
