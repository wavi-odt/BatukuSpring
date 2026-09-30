package org.example.batuku.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class GroqClient {

    private static final Logger log = LoggerFactory.getLogger(GroqClient.class);
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final String apiKey;
    private final String model;
    private final RestClient restClient;

    public GroqClient(@Value("${groq.api.key:}") String apiKey,
                      @Value("${groq.api.model:llama-3.3-70b-versatile}") String model) {
        this.apiKey = apiKey;
        this.model  = model;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public String generateInsight(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("groq.api.key não configurado — insight de IA desativado");
            return null;
        }
        try {
            Map<String, Object> body = Map.of(
                "model",      model,
                "temperature", 0.6,
                "max_tokens",  150,
                "messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user",   "content", userPrompt)
                )
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                .uri(GROQ_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(Map.class);

            if (response == null) return null;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) return null;

            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null) return null;

            return (String) message.get("content");

        } catch (RestClientResponseException e) {
            log.error("Groq API erro HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("Groq API falha inesperada: {}", e.getMessage());
            return null;
        }
    }
}
