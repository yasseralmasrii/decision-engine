package com.yasser.decisionengine.engine;

import com.yasser.decisionengine.model.Decision;
import com.yasser.decisionengine.model.ProposedAction;
import com.yasser.decisionengine.model.Signals;
import com.yasser.decisionengine.model.Verdict;
import com.yasser.decisionengine.signals.SignalProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Picks the right signal provider for the domain, then asks the policy engine. */
@Service
public class DecisionService {
    private final Map<String, SignalProvider> providers;
    private final PolicyEngine engine;
    public DecisionService(List<SignalProvider> providers, PolicyEngine engine) {
        this.providers = providers.stream()
                .collect(Collectors.toMap(SignalProvider::domain, Function.identity()));
        this.engine = engine;
    }
    public Decision decide(ProposedAction action) {
        SignalProvider provider = providers.get(action.domain());
        if (provider == null) {
            Signals none = new Signals(0, 1, false, List.of(), List.of(), 0);
            return new Decision(Verdict.REFUSE, none, "unknown-domain",
                    "No signal provider for domain '" + action.domain() + "'. Refusing to guess.");
        }
        return engine.decide(action, provider.compute(action));
    }
}