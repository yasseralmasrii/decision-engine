package com.yasser.decisionengine.engine;

import com.yasser.decisionengine.model.Decision;
import com.yasser.decisionengine.model.ProposedAction;
import com.yasser.decisionengine.model.Signals;
import com.yasser.decisionengine.model.Verdict;
import org.springframework.stereotype.Component;
import java.util.List;

/** Checks rules in order. The first match decides. No match means a human decides. */
@Component
public class PolicyEngine {
    static final double CRITICAL_RISK = 0.9;
    static final double HIGH_RISK = 0.6;
    static final double MIN_CONFIDENCE = 0.5;
    static final double EXECUTE_CONFIDENCE = 0.8;
    static final double MAX_AUTO_COST = 500;
    private final List<Rule> rules = List.of(
            new Rule("critical-irreversible", Verdict.REFUSE,
                    "Risk is critical and the action cannot be undone.",
                    (a, s) -> s.risk() >= CRITICAL_RISK && !s.reversible()),
            new Rule("missing-info", Verdict.ASK,
                    "Required information is missing.",
                    (a, s) -> !s.missingInfo().isEmpty()),
            new Rule("weak-evidence", Verdict.DEFER,
                    "Confidence is too low. Wait for more evidence.",
                    (a, s) -> s.confidence() < MIN_CONFIDENCE),
            new Rule("needs-human", Verdict.ESCALATE,
                    "High risk, irreversible, or expensive if wrong.",
                    (a, s) -> s.risk() >= HIGH_RISK
                            || !s.reversible()
                            || s.costOfError() > MAX_AUTO_COST),

            new Rule("safe-to-act", Verdict.EXECUTE,
                    "Low risk, reversible, and confident.",
                    (a, s) -> s.confidence() >= EXECUTE_CONFIDENCE)
    );
    public Decision decide(ProposedAction action, Signals signals) {
        for (Rule rule : rules) {
            if (rule.matches(action, signals)) {
                return new Decision(rule.verdict(), signals, rule.name(), rule.reason());
            }
        }
        return new Decision(Verdict.ESCALATE, signals, "fallback",
                "No rule was confident enough to decide, so a human decides.");
    }
}