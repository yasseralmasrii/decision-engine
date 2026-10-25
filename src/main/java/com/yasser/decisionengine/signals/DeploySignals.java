package com.yasser.decisionengine.signals;

import com.yasser.decisionengine.model.ProposedAction;
import com.yasser.decisionengine.model.Signals;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class DeploySignals implements SignalProvider {
    @Override
    public String domain() { return "deploy"; }
    @Override
    public Signals compute(ProposedAction action) {
        var p = Params.of(action);
        List<String> evidence = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        String service = Params.str(p, "service");
        String env = Params.str(p, "environment");
        Boolean testsPassed = Params.bool(p, "testsPassed");   // null = still running
        boolean rollback = Boolean.TRUE.equals(Params.bool(p, "rollbackAvailable"));
        double changedFiles = Params.num(p, "changedFiles", 0);
        if (service == null) missing.add("service");
        if (env == null) missing.add("environment");
        boolean prod = "production".equals(env);
        double confidence;
        double risk = prod ? 0.5 : 0.1;
        if (testsPassed == null) {
            confidence = 0.3;
            evidence.add("Tests still running");
        } else if (testsPassed) {
            confidence = 0.9;
            evidence.add("All tests passed");
        } else {
            confidence = 0.1;
            risk += 0.45;
            evidence.add("Tests FAILED");
        }
        if (changedFiles > 50) {
            risk += 0.2;
            evidence.add((int) changedFiles + " files changed (large change)");
        }
        evidence.add(rollback ? "Rollback available" : "No rollback plan");
        double cost = prod ? (rollback ? 300 : 2000) : 20;
        return new Signals(confidence, Params.clamp(risk), rollback, evidence, missing, cost);
    }
}