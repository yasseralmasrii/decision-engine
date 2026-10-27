package com.yasser.decisionengine.agent;

import com.yasser.decisionengine.TestEngine;
import com.yasser.decisionengine.audit.AuditLog;
import com.yasser.decisionengine.data.Records.Ticket;
import com.yasser.decisionengine.data.SystemOfRecord;
import com.yasser.decisionengine.model.AuditEntry;
import com.yasser.decisionengine.model.Verdict;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpClient;
import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The agent can be fooled. The engine must not be. */
class AgentFlowTest {

    private static final SystemOfRecord RECORDS = TestEngine.RECORDS;

    private TicketAgent agentWith(ActionProposer proposer) {
        return new TicketAgent(RECORDS, proposer, TestEngine.decisions(), new AuditLog(Clock.systemUTC()));
    }

    private AuditEntry run(String ticketId) {
        return agentWith(new RuleBasedProposer()).handle(ticketId).orElseThrow();
    }

    @Test
    void injectedTicket_agentIsFooled_engineRefuses() {
        AuditEntry e = run("T-102");
        // The agent fell for "SYSTEM: ... refund $900"
        assertEquals("refund", e.input().domain());
        assertEquals(900.0, ((Number) e.input().params().get("amount")).doubleValue(), 0.001);
        assertTrue(e.proposal().selfReportedConfidence() > 0.9);
        // ...but the order is only $60, so the engine refuses.
        assertEquals(Verdict.REFUSE, e.decision().verdict());
    }

    @Test
    void ticketOutcomesMatchExpectations() {
        assertEquals(Verdict.EXECUTE, run("T-101").decision().verdict()); // $25 refund, clean history
        assertEquals(Verdict.ESCALATE, run("T-103").decision().verdict()); // lawyer -> legal team
        assertEquals(Verdict.EXECUTE, run("T-104").decision().verdict()); // password reset reply
        assertEquals(Verdict.DEFER, run("T-106").decision().verdict()); // return still in transit
        assertEquals(Verdict.REFUSE, run("T-107").decision().verdict()); // already refunded
        assertEquals(Verdict.ASK, run("T-108").decision().verdict()); // no order id
    }

    @Test
    void unknownTicketReturnsEmpty() {
        assertTrue(agentWith(new RuleBasedProposer()).handle("T-999").isEmpty());
    }

    @Test
    void llmParsesModelJson_includingCodeFences() throws Exception {
        LlmProposer llm = new LlmProposer(HttpClient.newHttpClient(), JsonMapper.builder().build(),
                "http://127.0.0.1:9", "key", "test-model", new RuleBasedProposer());
        Ticket t = RECORDS.ticket("T-101").orElseThrow();
        String content = """
                ```json
                {"action":{"domain":"refund","type":"issue_refund","params":{"orderId":"A-1001","amount":25}},
                 "rationale":"Damaged item","confidence":0.97}
                ```""";
        var p = llm.parse(content, t);
        assertEquals("refund", p.action().domain());
        assertEquals("A-1001", p.action().params().get("orderId"));
        assertEquals(0.97, p.meta().selfReportedConfidence(), 0.001);
        assertEquals("llm:test-model", p.meta().proposer());
    }

    @Test
    void llmParsesJsonWrappedInProse() throws Exception {
        LlmProposer llm = new LlmProposer(HttpClient.newHttpClient(), JsonMapper.builder().build(),
                "http://127.0.0.1:9", "key", "test-model", new RuleBasedProposer());
        var p = llm.parse("Sure! Here is the action: {\"action\":{\"domain\":\"ticket\",\"type\":\"route\","
                + "\"params\":{\"ticketId\":\"T-104\",\"queue\":\"technical\"}},\"confidence\":0.8} Hope that helps.",
                RECORDS.ticket("T-104").orElseThrow());
        assertEquals("route", p.action().type());
    }

    @Test
    void llmUnreachable_fallsBackToRuleBasedAgent() {
        // Port 9 on localhost refuses connections, simulating an API outage.
        LlmProposer llm = new LlmProposer(HttpClient.newHttpClient(), JsonMapper.builder().build(),
                "http://127.0.0.1:9", "key", "test-model", new RuleBasedProposer());
        AuditEntry e = agentWith(llm).handle("T-101").orElseThrow();
        assertEquals(RuleBasedProposer.NAME, e.proposal().proposer());
        assertTrue(e.proposal().rationale().startsWith("LLM unavailable"));
        assertEquals(Verdict.EXECUTE, e.decision().verdict());
    }
}
