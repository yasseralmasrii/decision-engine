package com.yasser.decisionengine.engine;

import com.yasser.decisionengine.model.Decision;
import com.yasser.decisionengine.model.RuleCheck;
import com.yasser.decisionengine.model.Signals;
import com.yasser.decisionengine.model.Verdict;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Deterministic policy: rules are checked in order, the first match decides.
 * Every rule is still evaluated so the trace shows the full picture.
 *
 * Key design choice: REFUSE comes ONLY from explicit hard-policy violations.
 * Risk on its own can at most ESCALATE, so a large but legitimate request
 * goes to a human instead of being silently denied.
 */
@Component
public class PolicyEngine {

    public static final String POLICY_VERSION = "2026.10-2";

    static final double EXECUTE_CONFIDENCE = 0.8;
    static final double HIGH_RISK = 0.6;
    static final double MAX_AUTO_COST_USD = 500;
    static final double MAX_IRREVERSIBLE_AUTO_COST_USD = 100;

    private final List<Rule> rules = List.of(
            new Rule("policy-violation", Verdict.REFUSE,
                    "A hard policy forbids this action.",
                    s -> !s.violations().isEmpty(),
                    s -> s.violations().isEmpty() ? "no violations"
                            : String.join("; ", s.violations())),

            new Rule("missing-info", Verdict.ASK,
                    "Information only the requester can provide is missing.",
                    s -> !s.missingInfo().isEmpty(),
                    s -> s.missingInfo().isEmpty() ? "nothing missing"
                            : "missing: " + String.join(", ", s.missingInfo())),

            new Rule("pending-evidence", Verdict.DEFER,
                    "Evidence is still arriving; waiting will resolve it.",
                    s -> !s.pending().isEmpty(),
                    s -> s.pending().isEmpty() ? "nothing pending"
                            : "waiting on: " + String.join(", ", s.pending())),

            new Rule("low-confidence", Verdict.ESCALATE,
                    "Not confident enough to act alone.",
                    s -> s.confidence() < EXECUTE_CONFIDENCE,
                    s -> fmt("confidence %.2f (needs >= %.2f)", s.confidence(), EXECUTE_CONFIDENCE)),

            new Rule("high-stakes", Verdict.ESCALATE,
                    "High risk, or too costly to get wrong without a human.",
                    PolicyEngine::isHighStakes,
                    PolicyEngine::explainStakes),

            new Rule("safe-to-act", Verdict.EXECUTE,
                    "Confident, low risk, and cheap to be wrong.",
                    s -> true,
                    s -> "default: applies when no earlier rule matches"));

    public Decision decide(Signals signals) {
        List<RuleCheck> trace = new ArrayList<>();
        Rule winner = null;
        for (Rule rule : rules) {
            boolean matched = rule.matches(signals);
            boolean decides = winner == null && matched;
            trace.add(new RuleCheck(rule.name(), rule.verdict(), matched, decides, rule.explain(signals)));
            if (decides)
                winner = rule;
        }
        // The last rule always matches, so winner is never null.
        return new Decision(winner.verdict(), winner.name(), winner.reason(),
                nextStep(winner.verdict(), signals), signals, trace, POLICY_VERSION);
    }

    public List<Rule> rules() {
        return rules;
    }

    private static boolean isHighStakes(Signals s) {
        return s.risk() >= HIGH_RISK
                || s.costOfErrorUsd() > MAX_AUTO_COST_USD
                || (!s.reversible() && s.costOfErrorUsd() > MAX_IRREVERSIBLE_AUTO_COST_USD);
    }

    private static String explainStakes(Signals s) {
        List<String> reasons = new ArrayList<>();
        if (s.risk() >= HIGH_RISK)
            reasons.add(fmt("risk %.2f >= %.2f", s.risk(), HIGH_RISK));
        if (s.costOfErrorUsd() > MAX_AUTO_COST_USD)
            reasons.add(fmt("cost $%.2f > $%.0f", s.costOfErrorUsd(), MAX_AUTO_COST_USD));
        if (!s.reversible() && s.costOfErrorUsd() > MAX_IRREVERSIBLE_AUTO_COST_USD)
            reasons.add(fmt("irreversible and cost $%.2f > $%.0f", s.costOfErrorUsd(), MAX_IRREVERSIBLE_AUTO_COST_USD));
        return reasons.isEmpty()
                ? fmt("risk %.2f, cost $%.2f, %s", s.risk(), s.costOfErrorUsd(),
                        s.reversible() ? "reversible" : "irreversible")
                : String.join("; ", reasons);
    }

    private static String nextStep(Verdict verdict, Signals s) {
        return switch (verdict) {
            case EXECUTE ->
                "Proceed automatically. " + (s.reversible() ? "Can be undone if needed." : "Cannot be undone.");
            case ASK -> "Ask the requester for: " + String.join(", ", s.missingInfo()) + ".";
            case DEFER -> "Wait for " + String.join(", ", s.pending()) + ", then re-evaluate.";
            case ESCALATE -> "Route to " + s.approver() + " for approval.";
            case REFUSE -> "Do not act. Blocked by: " + String.join("; ", s.violations()) + ".";
        };
    }

    /**
     * Locale.ROOT so 0.80 never prints as "0,80" on machines with other locales.
     */
    private static String fmt(String pattern, Object... args) {
        return String.format(Locale.ROOT, pattern, args);
    }
}
