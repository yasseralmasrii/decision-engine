package com.yasser.decisionengine.signals;

import com.yasser.decisionengine.data.Records.Customer;
import com.yasser.decisionengine.data.Records.Ticket;
import com.yasser.decisionengine.data.SystemOfRecord;
import com.yasser.decisionengine.model.ProposedAction;
import com.yasser.decisionengine.model.Signals;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/**
 * Support ticket triage: route, send_reply, close.
 * Confidence comes from the engine's OWN classifier agreeing with the agent,
 * never from the agent's self-reported confidence.
 */
@Component
public class TicketSignals implements SignalProvider {

    static final Set<String> TYPES = Set.of("route", "send_reply", "close");

    private final SystemOfRecord records;

    public TicketSignals(SystemOfRecord records) {
        this.records = records;
    }

    @Override
    public String domain() { return "ticket"; }

    @Override
    public Signals compute(ProposedAction action) {
        var p = action.params();
        var s = new SignalsBuilder().approver("support-lead");
        String type = action.type();

        if (type == null || !TYPES.contains(type)) {
            return s.violation("Unsupported ticket action '" + type + "' (allowed: " + TYPES + ")").build();
        }
        String ticketId = Params.str(p, "ticketId");
        if (ticketId == null) return s.missing("ticketId").build();

        Optional<Ticket> found = records.ticket(ticketId);
        if (found.isEmpty()) {
            return s.missing("a valid ticketId ('" + ticketId + "' not found)").build();
        }
        Ticket ticket = found.get();
        Customer customer = records.customer(ticket.customerId()).orElse(null);
        boolean vip = customer != null && "vip".equals(customer.tier());

        String queue = Params.str(p, "queue");
        if ("route".equals(type) && queue == null) s.missing("target queue");
        if ("send_reply".equals(type) && Params.str(p, "reply") == null) s.missing("reply text");

        // --- Independent check: does the engine's own classifier agree? ---
        Optional<String> engineCategory = TextChecks.classify(ticket.text());
        if ("route".equals(type) && queue != null) {
            if (engineCategory.isEmpty()) {
                s.confidence(0.6).evidence("Engine could not classify the ticket independently");
            } else if (engineCategory.get().equalsIgnoreCase(queue)) {
                s.confidence(0.85).evidence("Engine's own classifier agrees: " + engineCategory.get());
            } else {
                s.confidence(0.4).evidence("Engine classifies as '" + engineCategory.get()
                        + "' but agent proposed '" + queue + "'");
            }
        } else {
            s.confidence(engineCategory.isPresent() ? 0.85 : 0.6)
             .evidence("Engine classification: " + engineCategory.orElse("unclear"));
        }

        // --- Risk ---
        s.risk(vip ? 0.4 : 0.1);
        if (vip) s.evidence("VIP customer");
        if (TextChecks.hasLegalLanguage(ticket.text())) {
            s.raiseRiskTo(0.7).approver("legal-team").evidence("Legal or chargeback language detected");
        }
        if (TextChecks.looksLikeInjection(ticket.text())) {
            s.raiseRiskTo(0.95).approver("security-team")
             .evidence("Ticket text contains instructions aimed at the AI (possible prompt injection)");
            if (!"route".equals(type)) {
                s.violation("No automated replies or closures on tickets containing AI-directed instructions");
            }
        }

        boolean reversible = !"send_reply".equals(type);   // a sent email can't be unsent
        s.reversible(reversible)
         .evidence(reversible ? "Action can be undone" : "A sent reply cannot be unsent")
         .costOfErrorUsd(vip ? 200 : 20);

        return s.build();
    }
}
