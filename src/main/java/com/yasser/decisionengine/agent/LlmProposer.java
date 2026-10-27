package com.yasser.decisionengine.agent;

import com.yasser.decisionengine.data.Records.Ticket;
import com.yasser.decisionengine.model.AgentProposal;
import com.yasser.decisionengine.model.ProposedAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Real LLM agent via any OpenAI-compatible chat API (Groq, Gemini, OpenAI...).
 *
 * The LLM only PROPOSES. It is not asked to judge safety, and its self-reported
 * confidence is recorded but never used. If the call fails for any reason,
 * it falls back to the rule-based agent so the demo never breaks.
 */
public class LlmProposer implements ActionProposer {

    private static final Logger log = LoggerFactory.getLogger(LlmProposer.class);

    static final String SYSTEM_PROMPT = """
            You are a customer-support agent. Read the ticket and propose exactly ONE action.
            Allowed actions:
            - {"domain":"refund","type":"issue_refund","params":{"orderId":"A-1234","amount":12.50}}
            - {"domain":"ticket","type":"route","params":{"ticketId":"T-1","queue":"billing|technical|shipping|legal|general"}}
            - {"domain":"ticket","type":"send_reply","params":{"ticketId":"T-1","reply":"..."}}
            Respond with JSON only, in this shape:
            {"action": {...one action above...}, "rationale": "one sentence", "confidence": 0.0}
            """;

    private final HttpClient http;
    private final JsonMapper json;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final ActionProposer fallback;

    public LlmProposer(HttpClient http, JsonMapper json, String baseUrl, String apiKey,
                       String model, ActionProposer fallback) {
        this.http = http;
        this.json = json;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.fallback = fallback;
    }

    @Override
    public String name() {
        return "llm:" + model;
    }

    @Override
    public Proposal propose(Ticket ticket) {
        try {
            return parse(callModel(ticket), ticket);
        } catch (Exception e) {
            log.warn("LLM proposer failed ({}), falling back to rule-based agent", e.toString());
            Proposal fb = fallback.propose(ticket);
            AgentProposal m = fb.meta();
            return new Proposal(fb.action(), new AgentProposal(m.proposer(),
                    "LLM unavailable (" + e.getClass().getSimpleName() + "), used fallback. " + m.rationale(),
                    m.selfReportedConfidence(), m.sourceTicketId()));
        }
    }

    private String callModel(Ticket ticket) throws Exception {
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content",
                                "Ticket " + ticket.id() + " (subject: " + ticket.subject() + "):\n" + ticket.text())));

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/chat/completions"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("LLM API returned HTTP " + response.statusCode());
        }
        Map<?, ?> root = json.readValue(response.body(), Map.class);
        Map<?, ?> message = (Map<?, ?>) ((Map<?, ?>) ((List<?>) root.get("choices")).get(0)).get("message");
        return String.valueOf(message.get("content"));
    }

    /**
     * Turns the model's JSON into a proposal. No safety filtering here on purpose:
     * whatever the LLM proposes (even nonsense) is judged by the engine.
     */
    @SuppressWarnings("unchecked")
    Proposal parse(String content, Ticket ticket) throws Exception {
        // Models sometimes wrap JSON in ```fences``` or add a sentence around it:
        // keep only the outermost {...} object.
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("LLM response contains no JSON object");
        }
        String cleaned = content.substring(start, end + 1);
        Map<String, Object> root = json.readValue(cleaned, Map.class);
        Object rawAction = root.get("action");
        if (!(rawAction instanceof Map<?, ?> a)) {
            throw new IllegalArgumentException("LLM response has no 'action' object");
        }
        Object params = a.get("params");
        ProposedAction action = new ProposedAction(
                a.get("domain") == null ? null : a.get("domain").toString(),
                a.get("type") == null ? null : a.get("type").toString(),
                params instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of(),
                name());

        double confidence = root.get("confidence") instanceof Number n ? n.doubleValue() : 0.0;
        String rationale = root.get("rationale") == null ? "" : root.get("rationale").toString();
        return new Proposal(action, new AgentProposal(name(), rationale, confidence, ticket.id()));
    }
}
