package com.yasser.decisionengine.demo;

import com.yasser.decisionengine.agent.TicketAgent;
import com.yasser.decisionengine.audit.AuditLog;
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
    static final String FEATURED_TICKET = "T-102";

    private final boolean enabled;
    private final DecisionService decisions;
    private final AuditLog audit;
    private final TicketAgent agent;

    public DemoSeeder(@Value("${demo.seed:true}") boolean enabled, DecisionService decisions,
            AuditLog audit, TicketAgent agent) {
        this.enabled = enabled;
        this.decisions = decisions;
        this.audit = audit;
        this.agent = agent;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled)
            return;
        for (var scenario : Scenarios.ALL) {
            audit.record(scenario.action(), decisions.preview(scenario.action()));
        }
        // One real agent run on the featured ticket, so the page opens on a full case.
        // Only this ticket, so seeding spends at most one LLM call and approves
        // nothing.
        agent.handle(FEATURED_TICKET);
        log.info("Demo seed complete: {} audit entries", audit.recent(Integer.MAX_VALUE).size());
    }
}
