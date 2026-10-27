package com.yasser.decisionengine.model;

import java.util.List;

/**
 * What the engine measured about an action, using facts it looked up itself.
 *
 * @param confidence     0..1, how sure the engine is that acting is correct
 * @param risk           0..1, how bad it is if acting turns out wrong
 * @param reversible     whether the action can be undone
 * @param costOfErrorUsd estimated impact in USD if the action is wrong
 * @param evidence       facts that were checked
 * @param missingInfo    facts only the requester can supply (leads to ASK)
 * @param pending        facts that will arrive with time (leads to DEFER)
 * @param violations     hard policy breaches (leads to REFUSE)
 * @param approver       who should judge it if escalated
 */
public record Signals(
        double confidence,
        double risk,
        boolean reversible,
        double costOfErrorUsd,
        List<String> evidence,
        List<String> missingInfo,
        List<String> pending,
        List<String> violations,
        String approver
) {
    public Signals {
        evidence = copy(evidence);
        missingInfo = copy(missingInfo);
        pending = copy(pending);
        violations = copy(violations);
        approver = (approver == null || approver.isBlank()) ? "human-reviewer" : approver;
    }

    private static List<String> copy(List<String> list) {
        return list == null ? List.of() : List.copyOf(list);
    }
}
