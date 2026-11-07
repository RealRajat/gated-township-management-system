package com.township.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.township.dto.ComplaintClassificationDto;
import com.township.entity.ComplaintCategory;
import com.township.entity.ComplaintPriority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Gemini AI Classifier with graceful fallback to RuleBasedClassificationEngine.
 */
@Service
public class GeminiComplaintClassificationService {

    private static final Logger log = LoggerFactory.getLogger(GeminiComplaintClassificationService.class);

    private final RuleBasedClassificationEngine fallbackEngine;
    private final ObjectMapper objectMapper;
    private final String geminiApiKey;
    private final HttpClient httpClient;

    public GeminiComplaintClassificationService(
            RuleBasedClassificationEngine fallbackEngine,
            @Value("${gemini.api.key:#{null}}") String configuredApiKey) {
        this.fallbackEngine = fallbackEngine;
        this.objectMapper = new ObjectMapper();
        // Read either from configured property or environment variable
        String envKey = System.getenv("GEMINI_API_KEY");
        this.geminiApiKey = (configuredApiKey != null && !configuredApiKey.isBlank()) ? configuredApiKey : envKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .build();

        if (this.geminiApiKey != null && !this.geminiApiKey.isBlank()) {
            log.info("Gemini AI API key detected. External AI complaint classification enabled.");
        } else {
            log.info("No GEMINI_API_KEY detected. Using certified Rule-Based semantic classification engine.");
        }
    }

    public boolean isGeminiConfigured() {
        return geminiApiKey != null && !geminiApiKey.isBlank();
    }

    public ComplaintClassificationDto classify(String title, String description) {
        if (!isGeminiConfigured()) {
            return fallbackEngine.classify(title, description);
        }

        try {
            String promptText = String.format(
                    "You are a residential township facility management triage assistant in Pune, India.\n" +
                    "Classify this resident complaint into:\n" +
                    "- Category: exactly one of [PLUMBING, ELECTRICAL, LIFT, SECURITY, HOUSEKEEPING, PARKING, CIVIL_MAINTENANCE, OTHER]\n" +
                    "- Priority: exactly one of [LOW, MEDIUM, HIGH, CRITICAL]\n" +
                    "- Confidence: integer between 0 and 100\n" +
                    "- Reason: brief sentence\n" +
                    "Title: %s\n" +
                    "Description: %s\n" +
                    "Output JSON only with keys: category, priority, confidence, reason.",
                    title, description
            );

            String requestBody = objectMapper.writeValueAsString(new GeminiPayload(promptText));
            String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + geminiApiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                String responseText = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

                // Strip markdown JSON wrapping if present
                String cleanJson = responseText.replaceAll("(?s)```json\\s*(.*?)\\s*```", "$1").trim();
                JsonNode parsed = objectMapper.readTree(cleanJson);

                ComplaintCategory cat = parseCategory(parsed.path("category").asText());
                ComplaintPriority prio = parsePriority(parsed.path("priority").asText());
                int conf = parsed.path("confidence").asInt(90);
                String reason = parsed.path("reason").asText("AI classified via Gemini 1.5 Flash.");

                return new ComplaintClassificationDto(
                        cat,
                        prio,
                        conf,
                        false,
                        null,
                        reason,
                        "GEMINI_AI"
                );
            } else {
                log.warn("Gemini API returned status {}. Falling back to Rule-Based engine.", response.statusCode());
                return fallbackEngine.classify(title, description);
            }
        } catch (Exception e) {
            log.warn("Gemini classification failed ({}), using fallback: {}", e.getClass().getSimpleName(), e.getMessage());
            return fallbackEngine.classify(title, description);
        }
    }

    private ComplaintCategory parseCategory(String text) {
        if (text == null) return ComplaintCategory.OTHER;
        try {
            return ComplaintCategory.valueOf(text.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return ComplaintCategory.OTHER;
        }
    }

    private ComplaintPriority parsePriority(String text) {
        if (text == null) return ComplaintPriority.MEDIUM;
        try {
            return ComplaintPriority.valueOf(text.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return ComplaintPriority.MEDIUM;
        }
    }

    private record GeminiPayload(String prompt) {
        public Object getContents() {
            return new Object[] {
                    new Object() {
                        public Object[] getParts() {
                            return new Object[] {
                                    new Object() {
                                        public String getText() {
                                            return prompt;
                                        }
                                    }
                            };
                        }
                    }
            };
        }
    }
}
