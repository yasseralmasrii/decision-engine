package com.yasser.decisionengine.signals;

import com.yasser.decisionengine.data.Records.CiRun;
import com.yasser.decisionengine.data.Records.Service;
import com.yasser.decisionengine.data.SystemOfRecord;
import com.yasser.decisionengine.model.ProposedAction;
import com.yasser.decisionengine.model.Signals;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/**
 * Code deploys. The agent supplies service + environment + commit;
 * CI status, rollback ability and change freezes are looked up.
 */
@Component
public class DeploySignals implements SignalProvider {

    static final int LARGE_CHANGE_FILES = 50;
    static final Set<String> ENVIRONMENTS = Set.of("staging", "production");

    private final SystemOfRecord records;

    public DeploySignals(SystemOfRecord records) {
        this.records = records;
    }

    @Override
    public String domain() { return "deploy"; }

    @Override
    public Signals compute(ProposedAction action) {
        var p = action.params();
        var s = new SignalsBuilder().approver("release-manager");

        String serviceName = Params.str(p, "service");
        String env = Params.str(p, "environment");
        String commit = Params.str(p, "commit");

        if (serviceName == null) s.missing("service");
        if (env == null) s.missing("environment");
        if (commit == null) s.missing("commit");
        if (serviceName == null || env == null || commit == null) return s.build();

        if (!ENVIRONMENTS.contains(env)) {
            return s.violation("Unknown environment '" + env + "'").build();
        }
        Optional<Service> svc = records.service(serviceName);
        if (svc.isEmpty()) {
            return s.missing("a known service ('" + serviceName + "' is not in the service catalog)").build();
        }
        Optional<CiRun> ci = records.ciRun(commit);
        if (ci.isEmpty()) {
            return s.missing("a commit with a CI run ('" + commit + "' has none)").build();
        }
        Service service = svc.get();
        CiRun run = ci.get();
        boolean prod = "production".equals(env);

        // --- Hard policies ---
        if (!run.service().equals(service.name())) {
            s.violation("Commit " + commit + " was built for '" + run.service() + "', not '" + service.name() + "'");
        }
        if (prod && service.freezeDaysRemaining() > 0) {
            s.violation("Change freeze on '" + service.name() + "' for "
                    + service.freezeDaysRemaining() + " more days");
        }

        // --- CI status ---
        s.risk(prod ? 0.5 : 0.1);
        switch (run.status()) {
            case "PASSED" -> s.confidence(0.9)
                    .evidence(run.id() + " passed (" + run.testsRun() + " tests)");
            case "RUNNING" -> s.confidence(0.5)
                    .pending(run.id() + " (commit " + commit + ") to finish");
            case "FAILED" -> {
                s.confidence(0.1).evidence(run.id() + " FAILED");
                if (prod) s.violation("Production deploys require passing CI (" + run.id() + " failed)");
                else s.risk(s.risk() + 0.3);
            }
            default -> s.confidence(0.3).evidence("Unknown CI status '" + run.status() + "'");
        }

        // --- Blast radius ---
        if (run.changedFiles() > LARGE_CHANGE_FILES) {
            s.risk(s.risk() + 0.2).evidence(run.changedFiles() + " files changed (large change)");
        } else {
            s.evidence(run.changedFiles() + " files changed");
        }
        if (service.critical()) s.risk(s.risk() + 0.1).evidence("Critical service");

        s.reversible(service.rollbackAvailable())
         .evidence(service.rollbackAvailable() ? "Rollback available" : "No rollback plan")
         .costOfErrorUsd(prod ? (service.rollbackAvailable() ? 300 : 2000) : 20);

        return s.build();
    }
}
