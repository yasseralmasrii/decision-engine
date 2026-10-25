package com.yasser.decisionengine.api;

import com.yasser.decisionengine.audit.AuditLog;
import com.yasser.decisionengine.engine.DecisionService;
import com.yasser.decisionengine.model.AuditEntry;
import com.yasser.decisionengine.model.Decision;
import com.yasser.decisionengine.model.ProposedAction;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DecisionController {
    private final DecisionService service;
    private final AuditLog audit;
    public DecisionController(DecisionService service, AuditLog audit) {
        this.service = service;
        this.audit = audit;
    }
    /** Submit a proposed action. Returns the decision plus its audit record. */
    @PostMapping("/decide")
    public ResponseEntity<?> decide(@RequestBody ProposedAction action) {
        if (action.domain() == null || action.domain().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Field 'domain' is required"));
        }
        Decision decision = service.decide(action);
        return ResponseEntity.ok(audit.record(action, decision));
    }
    /** The audit trail, newest first. */
    @GetMapping("/audit")
    public List<AuditEntry> audit(@RequestParam(defaultValue = "50") int limit) {
        return audit.recent(Math.min(limit, 500));
    }
    /** One decision, by id. */
    @GetMapping("/audit/{id}")
    public ResponseEntity<AuditEntry> one(@PathVariable String id) {
        return audit.find(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}