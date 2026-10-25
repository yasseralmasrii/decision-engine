package com.yasser.decisionengine.model;

/**
 * The five possible outcomes of the decision layer.
 * Ordered from least to most cautious.
 */
public enum Verdict {
    EXECUTE("Safe to act: low risk, enough evidence, reversible."),
    ASK("Missing information: ask the requester before acting."),
    DEFER("Not yet: wait for more evidence or a better time."),
    ESCALATE("Allowed in principle, but a human must approve."),
    REFUSE("Not allowed: violates policy, no matter how confident.");
    private final String meaning;
    Verdict(String meaning) {
        this.meaning = meaning;
    }
    public String meaning() {
        return meaning;
    }
}