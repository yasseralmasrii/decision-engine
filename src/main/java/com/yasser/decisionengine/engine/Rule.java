package com.yasser.decisionengine.engine;

import com.yasser.decisionengine.model.Signals;
import com.yasser.decisionengine.model.Verdict;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * One policy rule: "if this condition holds, return this verdict, for this
 * reason."
 * explain() states the numbers behind the result, for the decision trace.
 */
public record Rule(
        String name,
        Verdict verdict,
        String reason,
        Predicate<Signals> condition,
        Function<Signals, String> explainer) {
    public boolean matches(Signals signals) {
        return condition.test(signals);
    }

    public String explain(Signals signals) {
        return explainer.apply(signals);
    }
}
