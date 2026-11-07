package com.township.service.ai;

import com.township.dto.ComplaintClassificationDto;
import com.township.entity.ComplaintPriority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Primary Complaint Classification Service.
 * Evaluates mandatory safety keyword overrides first, ensuring safety escalations
 * cannot be downgraded by any AI model or rule fallback.
 */
@Service
@Primary
public class CompositeComplaintClassificationService implements ComplaintClassificationService {

    private static final Logger log = LoggerFactory.getLogger(CompositeComplaintClassificationService.class);

    private final SafetyKeywordRuleEngine safetyRuleEngine;
    private final GeminiComplaintClassificationService aiClassifier;

    public CompositeComplaintClassificationService(
            SafetyKeywordRuleEngine safetyRuleEngine,
            GeminiComplaintClassificationService aiClassifier) {
        this.safetyRuleEngine = safetyRuleEngine;
        this.aiClassifier = aiClassifier;
    }

    @Override
    public ComplaintClassificationDto classify(String title, String description) {
        // Step 1: Mandatory Safety Rule Engine Evaluation (HIGHEST PRECEDENCE)
        SafetyKeywordRuleEngine.SafetyMatch safetyMatch = safetyRuleEngine.evaluate(title, description);

        if (safetyMatch.isSafetyEscalation()) {
            log.warn("SAFETY ESCALATION TRIGGERED: Keyword '{}' in group '{}'. Enforcing CRITICAL priority.",
                    safetyMatch.matchedKeyword(), safetyMatch.groupName());

            return new ComplaintClassificationDto(
                    safetyMatch.suggestedCategory(),
                    ComplaintPriority.CRITICAL,
                    100,
                    true,
                    safetyMatch.matchedKeyword(),
                    "MANDATORY CRITICAL SAFETY OVERRIDE: Triggered by keyword '" + safetyMatch.matchedKeyword() +
                            "' (" + safetyMatch.groupName() + "). AI downgrades are strictly prevented.",
                    "SAFETY_RULE_ENGINE"
            );
        }

        // Step 2: AI / Semantic Rule-Based Evaluation (if no safety keywords)
        ComplaintClassificationDto result = aiClassifier.classify(title, description);

        // Step 3: Safety Invariant Check (Redundant safety guard)
        if (safetyMatch.isSafetyEscalation()) {
            return new ComplaintClassificationDto(
                    safetyMatch.suggestedCategory(),
                    ComplaintPriority.CRITICAL,
                    100,
                    true,
                    safetyMatch.matchedKeyword(),
                    "MANDATORY CRITICAL SAFETY OVERRIDE: Safety keywords cannot be downgraded.",
                    "SAFETY_RULE_ENGINE"
            );
        }

        return result;
    }
}

