package com.yasser.decisionengine.engine;

import com.yasser.decisionengine.model.Decision;
import com.yasser.decisionengine.model.ProposedAction;
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

    public Decision decide(ProposedAction action) {
        SignalProvider provider = action.domain() == null ? null : providers.get(action.domain());
        if (provider == null) {
            return engine.decide(new SignalsBuilder()
                    .confidence(0).risk(1)
                    .violation("Unknown domain '" + action.domain() + "' (known: " + domains()
                            + "). The engine refuses to guess")
                    .build());
        }
        return engine.decide(provider.compute(action));
    }

    public Set<String> domains() {
        return new TreeSet<>(providers.keySet());
    }
}
