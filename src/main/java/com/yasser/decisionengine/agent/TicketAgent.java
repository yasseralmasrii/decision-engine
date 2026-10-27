package com.yasser.decisionengine.agent;

import com.yasser.decisionengine.audit.AuditLog;
import com.yasser.decisionengine.data.SystemOfRecord;
import com.yasser.decisionengine.engine.DecisionService;
import com.yasser.decisionengine.model.AuditEntry;
import com.yasser.decisionengine.model.Decision;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * The full loop: AI agent reads a ticket and proposes, the engine decides,
 * the audit log records proposal + decision together.
 */
@Service
public class TicketAgent {

    private final SystemOfRecord records;
    private final ActionProposer proposer;
    private final DecisionService decisions;
    private final AuditLog audit;

    public TicketAgent(SystemOfRecord records, ActionProposer proposer,
            DecisionService decisions, AuditLog audit) {
        this.records = records;
        this.proposer = proposer;
        this.decisions = decisions;
        this.audit = audit;
    }

    /** Empty if the ticket does not exist. */
    public Optional<AuditEntry> handle(String ticketId) {
        return records.ticket(ticketId).map(ticket -> {
            ActionProposer.Proposal proposal = proposer.propose(ticket);
            Decision decision = decisions.decide(proposal.action());
            return audit.record(proposal.action(), decision, proposal.meta());
        });
    }

    public String proposerName() {
        return proposer.name();
    }
}
