package com.yasser.decisionengine.engine;

import com.yasser.decisionengine.model.ProposedAction;
import com.yasser.decisionengine.model.Signals;
import com.yasser.decisionengine.model.Verdict;
import java.util.function.BiPredicate;

/** One policy rule: "if this condition holds, return this verdict, for this reason." */
public record Rule(
        String name,
        Verdict verdict,
        String reason,
        BiPredicate<ProposedAction, Signals> condition
) {
    public boolean matches(ProposedAction action, Signals signals) {
        return condition.test(action, signals);
    }
}