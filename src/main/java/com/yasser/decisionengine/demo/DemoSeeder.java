package com.yasser.decisionengine.demo;

import com.yasser.decisionengine.agent.TicketAgent;
import com.yasser.decisionengine.audit.AuditLog;
import com.yasser.decisionengine.data.SystemOfRecord;
import com.yasser.decisionengine.engine.DecisionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * The audit log lives in memory and the free host sleeps after 15 minutes,
 * so a judge could open an empty audit page. This fills it at every startup.
 */
@Component
public class DemoSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoSeeder.class);

    private final boolean enabled;
    private final DecisionService decisions;
    private final AuditLog audit;
    private final TicketAgent agent;
    private final SystemOfRecord records;

    public DemoSeeder(@Value("${demo.seed:true}") boolean enabled, DecisionService decisions,
            AuditLog audit, TicketAgent agent, SystemOfRecord records) {
        this.enabled = enabled;
        this.decisions = decisions;
        this.audit = audit;
        this.agent = agent;
        this.records = records;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled)
            return;
        for (var scenario : Scenarios.ALL) {
            audit.record(scenario.action(), decisions.decide(scenario.action()));
        }
        // Agent runs use the rule-based agent at startup only if no LLM key is set,
        // so seeding never spends LLM quota.
        if (agent.proposerName().startsWith("rule-based")) {
            records.tickets().forEach(t -> agent.handle(t.id()));
        }
        log.info("Demo seed complete: {} audit entries", audit.recent(Integer.MAX_VALUE).size());
    }
}
