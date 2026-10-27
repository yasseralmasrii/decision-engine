package com.yasser.decisionengine.api;

import com.yasser.decisionengine.agent.TicketAgent;
import com.yasser.decisionengine.audit.AuditLog;
import com.yasser.decisionengine.audit.ChainStatus;
import com.yasser.decisionengine.data.Records.Ticket;
import com.yasser.decisionengine.data.SystemOfRecord;
import com.yasser.decisionengine.demo.Scenarios;
import com.yasser.decisionengine.engine.DecisionService;
import com.yasser.decisionengine.engine.PolicyEngine;
import com.yasser.decisionengine.model.AuditEntry;
import com.yasser.decisionengine.model.ProposedAction;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DecisionController {

    private final DecisionService decisions;
    private final PolicyEngine policy;
    private final AuditLog audit;
    private final TicketAgent agent;
    private final SystemOfRecord records;

    public DecisionController(DecisionService decisions, PolicyEngine policy, AuditLog audit,
            TicketAgent agent, SystemOfRecord records) {
        this.decisions = decisions;
        this.policy = policy;
        this.audit = audit;
        this.agent = agent;
        this.records = records;
    }

    // ---------- Core: submit an action, get a decision ----------

    @PostMapping("/decide")
    public ResponseEntity<?> decide(@RequestBody ProposedAction action) {
        if (action.domain() == null || action.domain().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Field 'domain' is required"));
        }
        return ResponseEntity.ok(audit.record(action, decisions.decide(action)));
    }

    // ---------- AI agent: read a ticket, propose, get judged ----------

    @PostMapping("/agent/tickets/{ticketId}")
    public ResponseEntity<AuditEntry> runAgent(@PathVariable("ticketId") String ticketId) {
        return agent.handle(ticketId).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/tickets")
    public List<Ticket> tickets() {
        return records.tickets();
    }

    // ---------- Curated scenarios for the demo page ----------

    @GetMapping("/scenarios")
    public List<Scenarios.Scenario> scenarios() {
        return Scenarios.ALL;
    }

    /**
     * Scenarios are dry runs: the ruling is recorded, but nothing is remembered as
     * executed.
     */
    @PostMapping("/scenarios/{id}/run")
    public ResponseEntity<AuditEntry> runScenario(@PathVariable("id") String id) {
        return Scenarios.find(id)
                .map(s -> audit.record(s.action(), decisions.preview(s.action())))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ---------- Audit trail ----------

    @GetMapping("/audit")
    public List<AuditEntry> recent(@RequestParam(name = "limit", defaultValue = "50") int limit) {
        return audit.recent(limit);
    }

    @GetMapping("/audit/verify")
    public ChainStatus verify() {
        return audit.verify();
    }

    @GetMapping("/audit/{id}")
    public ResponseEntity<AuditEntry> one(@PathVariable("id") String id) {
        return audit.find(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    // ---------- Transparency: what rules are in force ----------

    @GetMapping("/policy")
    public Map<String, Object> policy() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("version", PolicyEngine.POLICY_VERSION);
        out.put("agent", agent.proposerName());
        out.put("domains", decisions.domains());
        out.put("rules", policy.rules().stream()
                .map(r -> Map.of("name", r.name(), "verdict", r.verdict(), "reason", r.reason()))
                .toList());
        return out;
    }
}
