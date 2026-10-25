package com.yasser.decisionengine.model;

import java.util.Map;

/** What the agent wants to do. */
public record ProposedAction(
        String domain,              // e.g. "refund", "deploy", "ticket"
        String type,                // e.g. "issue_refund"
        Map<String, Object> params, // e.g. amount, orderId
        String requester            // who or what asked for it
) {}