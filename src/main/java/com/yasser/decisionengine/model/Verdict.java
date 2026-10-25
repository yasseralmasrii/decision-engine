package com.yasser.decisionengine.model;

/**
 * The five possible outcomes of the decision layer, ordered from least to most
 * cautious.
 *
 * ASK = information only the requester can give is missing.
 * DEFER = evidence is still arriving; waiting alone will resolve it.
 * ESCALATE = a human with authority must judge it.
 */
public enum Verdict {

    EXECUTE("Safe to act: confident, low risk, cheap to be wrong."),
    ASK("Missing information: ask the requester before acting."),
    DEFER("Not yet: evidence is still arriving, re-check later."),
    ESCALATE("Allowed in principle, but a human must approve."),
    REFUSE("Not allowed: a hard policy forbids it, however confident.");

    private final String meaning;

    Verdict(String meaning) {
        this.meaning = meaning;
    }

    public String meaning() {
        return meaning;
    }
}
