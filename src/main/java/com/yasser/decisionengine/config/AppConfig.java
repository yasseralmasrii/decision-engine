package com.yasser.decisionengine.config;

import com.yasser.decisionengine.agent.ActionProposer;
import com.yasser.decisionengine.agent.LlmProposer;
import com.yasser.decisionengine.agent.RuleBasedProposer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;

@Configuration
public class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    /** Injected so tests can freeze time. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /** Uses a real LLM when LLM_API_KEY is set, otherwise the rule-based agent. */
    @Bean
    public ActionProposer actionProposer(@Value("${llm.api-key:}") String apiKey,
            @Value("${llm.base-url:https://api.groq.com/openai/v1}") String baseUrl,
            @Value("${llm.model:llama-3.3-70b-versatile}") String model) {
        ActionProposer rules = new RuleBasedProposer();
        if (apiKey == null || apiKey.isBlank()) {
            log.info("No LLM_API_KEY set: using the rule-based agent");
            return rules;
        }
        log.info("Using LLM agent: {} at {}", model, baseUrl);
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        return new LlmProposer(http, JsonMapper.builder().build(), baseUrl, apiKey, model, rules);
    }
}
