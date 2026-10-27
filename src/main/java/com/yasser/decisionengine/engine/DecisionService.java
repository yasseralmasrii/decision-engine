package com.yasser.decisionengine.engine;

import com.yasser.decisionengine.model.Decision;
import com.yasser.decisionengine.model.ProposedAction;
import com.yasser.decisionengine.model.Verdict;
import com.yasser.decisionengine.signals.SignalProvider;
import com.yasser.decisionengine.signals.SignalsBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Picks the signal provider for the action's domain, then asks the policy
 * engine.
 *
 * decide() = a real request: an EXECUTE is remembered (e.g. refund ledger).
 * preview() = a dry run: same ruling, nothing remembered. Used by the scenario
 * library.
 */
@Service
public class DecisionService {

    private final Map<String, SignalProvider> providers;
    private final PolicyEngine engine;

    public DecisionService(List<SignalProvider> providers, PolicyEngine engine) {
        // toMap throws on duplicate domains: fail fast at startup, not at decision
        // time.
        this.providers = providers.stream()
                .collect(Collectors.toMap(SignalProvider::domain, Function.identity()));
        this.engine = engine;
    }

    /**
     * Synchronized so measuring and remembering happen as one step:
     * two identical refunds arriving together cannot both pass the balance check.
     */
    public synchronized Decision decide(ProposedAction action) {
        return evaluate(action, true);
    }

    public Decision preview(ProposedAction action) {
        return evaluate(action, false);
    }

    private Decision evaluate(ProposedAction action, boolean commit) {
        SignalProvider provider = action.domain() == null ? null : providers.get(action.domain());
        if (provider == null) {
            return engine.decide(new SignalsBuilder()
                    .confidence(0).risk(1)
                    .violation("Unknown domain '" + action.domain() + "' (known: " + domains()
                            + "). The engine refuses to guess")
                    .build());
        }
        Decision decision = engine.decide(provider.compute(action));
        if (commit && decision.verdict() == Verdict.EXECUTE) {
            provider.onExecuted(action);
        }
        return decision;
    }

    public Set<String> domains() {
        return new TreeSet<>(providers.keySet());
    }
}
