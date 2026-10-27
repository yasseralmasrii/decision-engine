package com.yasser.decisionengine.model;

import java.util.List;

/**
 * The engine's answer, with everything needed to explain and replay it.
 *
 * @param verdict       execute, ask, defer, escalate or refuse
 * @param ruleFired     the rule that decided
 * @param reasoning     why, in one sentence
 * @param nextStep      what should happen now (the question, the wait, the approver...)
 * @param signals       the measurements the decision was based on
 * @param trace         every rule, in order, and how it evaluated
 * @param policyVersion which rule set produced this decision
 */
public record Decision(
        Verdict verdict,
        String ruleFired,
        String reasoning,
        String nextStep,
        Signals signals,
        List<RuleCheck> trace,
        String policyVersion
) {
    public Decision {
        trace = trace == null ? List.of() : List.copyOf(trace);
    }
}
