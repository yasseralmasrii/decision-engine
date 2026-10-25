package com.yasser.decisionengine.model;

import java.time.Instant;

/** One permanent record per decision. */
public record AuditEntry(
        String id,
        Instant timestamp,
        ProposedAction input,
        Decision decision
) {}