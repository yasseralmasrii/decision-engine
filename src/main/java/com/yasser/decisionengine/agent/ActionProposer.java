package com.yasser.decisionengine.agent;

import com.yasser.decisionengine.data.Records.Ticket;
import com.yasser.decisionengine.model.AgentProposal;
import com.yasser.decisionengine.model.ProposedAction;

/**
 * An AI agent that reads a ticket and proposes ONE action.
 * It has no authority: every proposal goes through the decision engine.
 */
public interface ActionProposer {

    String name();

    Proposal propose(Ticket ticket);

    record Proposal(ProposedAction action, AgentProposal meta) {
    }
}
