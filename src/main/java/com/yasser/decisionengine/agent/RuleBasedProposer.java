package com.yasser.decisionengine.agent;

import com.yasser.decisionengine.data.Records.Ticket;
import com.yasser.decisionengine.model.AgentProposal;
import com.yasser.decisionengine.model.ProposedAction;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fallback agent used when no LLM key is configured (or the LLM call fails).
 *
 * It is deliberately GULLIBLE, like an LLM that falls for prompt injection:
 * it believes any order id and dollar amount written in the ticket.
 * That is the point: the agent can be fooled, the engine must not be.
 */
public class RuleBasedProposer implements ActionProposer {

    static final String NAME = "rule-based-agent";

    private static final Pattern ORDER_ID = Pattern.compile("\\bA-\\d{4}\\b");
    private static final Pattern AMOUNT = Pattern.compile("\\$\\s?(\\d+(?:\\.\\d{1,2})?)");

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Proposal propose(Ticket ticket) {
        String text = ticket.text() == null ? "" : ticket.text();
        String lower = text.toLowerCase(Locale.ROOT);

        if (lower.contains("refund") || lower.contains("money back")) {
            Map<String, Object> params = new LinkedHashMap<>();
            find(ORDER_ID, text, 0).ifPresent(id -> params.put("orderId", id));
            find(AMOUNT, text, 1).ifPresent(a -> params.put("amount", Double.parseDouble(a)));
            return proposal(new ProposedAction("refund", "issue_refund", params, NAME), ticket,
                    "Customer asked for a refund; taking the order and amount from the ticket.", 0.92);
        }

        if (lower.contains("password") || lower.contains("log in") || lower.contains("login")) {
            return proposal(new ProposedAction("ticket", "send_reply", Map.of(
                    "ticketId", ticket.id(),
                    "reply", "You can reset your password from Settings > Security > Reset password. "
                            + "If the link does not arrive within 10 minutes, reply here and we will help."),
                    NAME), ticket,
                    "Standard password-reset question; sending the help-center answer.", 0.95);
        }

        String queue = lower.contains("lawyer") || lower.contains("chargeback") ? "legal"
                : lower.contains("package") || lower.contains("tracking") ? "shipping"
                        : lower.contains("charge") || lower.contains("invoice") ? "billing"
                                : "general";
        return proposal(new ProposedAction("ticket", "route", Map.of("ticketId", ticket.id(), "queue", queue), NAME),
                ticket, "Routing to the '" + queue + "' queue based on keywords.", 0.85);
    }

    private static Optional<String> find(Pattern pattern, String text, int group) {
        Matcher m = pattern.matcher(text);
        return m.find() ? Optional.of(m.group(group)) : Optional.empty();
    }

    private static Proposal proposal(ProposedAction action, Ticket ticket, String rationale, double confidence) {
        return new Proposal(action, new AgentProposal(NAME, rationale, confidence, ticket.id()));
    }
}
