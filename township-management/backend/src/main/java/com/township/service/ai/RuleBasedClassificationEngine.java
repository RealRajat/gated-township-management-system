package com.township.service.ai;

import com.township.dto.ComplaintClassificationDto;
import com.township.entity.ComplaintCategory;
import com.township.entity.ComplaintPriority;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Intelligent semantic rule-based fallback classification engine.
 * Used when an external AI provider (Gemini) is not configured or unavailable.
 */
@Component
public class RuleBasedClassificationEngine {

    public ComplaintClassificationDto classify(String title, String description) {
        String combined = ((title != null ? title : "") + " " + (description != null ? description : "")).toLowerCase(Locale.ENGLISH);

        ComplaintCategory category = detectCategory(combined);
        ComplaintPriority priority = detectPriority(combined, category);
        int confidence = calculateConfidence(combined, category);

        return new ComplaintClassificationDto(
                category,
                priority,
                confidence,
                false,
                null,
                "Rule-based semantic keyword classifier matched category " + category + " with " + priority + " urgency.",
                "RULE_BASED_FALLBACK"
        );
    }

    private ComplaintCategory detectCategory(String text) {
        if (text.contains("lift") || text.contains("elevator") || text.contains("lift door") || text.contains("lift light") || text.contains("lift noise")) {
            return ComplaintCategory.LIFT;
        }
        if (text.contains("pipe") || text.contains("drain") || text.contains("sink") || text.contains("tap") ||
                text.contains("leak") || text.contains("flush") || text.contains("geyser") || text.contains("sewage") ||
                text.contains("faucet") || text.contains("low water") || text.contains("no water") || text.contains("overflow")) {
            return ComplaintCategory.PLUMBING;
        }
        if (text.contains("light") || text.contains("power") || text.contains("bulb") || text.contains("switch") ||
                text.contains("socket") || text.contains("tripping") || text.contains("mcb") || text.contains("voltage") ||
                text.contains("wiring") || text.contains("fuse") || text.contains("blackout")) {
            return ComplaintCategory.ELECTRICAL;
        }
        if (text.contains("guard") || text.contains("gate") || text.contains("cctv") || text.contains("visitor") ||
                text.contains("theft") || text.contains("security") || text.contains("boom barrier") || text.contains("stranger")) {
            return ComplaintCategory.SECURITY;
        }
        if (text.contains("garbage") || text.contains("trash") || text.contains("cleaning") || text.contains("dirty") ||
                text.contains("sweep") || text.contains("dustbin") || text.contains("corridor") || text.contains("staircase")) {
            return ComplaintCategory.HOUSEKEEPING;
        }
        if (text.contains("parking") || text.contains("car") || text.contains("vehicle") || text.contains("slot") ||
                text.contains("bike") || text.contains("blocked car") || text.contains("basement")) {
            return ComplaintCategory.PARKING;
        }
        if (text.contains("wall") || text.contains("ceiling") || text.contains("seepage") || text.contains("crack") ||
                text.contains("plaster") || text.contains("tiles") || text.contains("painting") || text.contains("window") ||
                text.contains("railing") || text.contains("door lock") || text.contains("roof")) {
            return ComplaintCategory.CIVIL_MAINTENANCE;
        }

        return ComplaintCategory.OTHER;
    }

    private ComplaintPriority detectPriority(String text, ComplaintCategory category) {
        if (text.contains("urgent") || text.contains("emergency") || text.contains("overflowing") ||
                text.contains("flooding") || text.contains("no water") || text.contains("complete blackout") ||
                text.contains("blocked main door") || text.contains("stolen") || text.contains("robbery")) {
            return ComplaintPriority.HIGH;
        }

        if (text.contains("flickering") || text.contains("slow drain") || text.contains("dripping") ||
                text.contains("noise") || text.contains("intercom") || text.contains("not working") ||
                text.contains("broken") || text.contains("damaged") || text.contains("smell")) {
            return ComplaintPriority.MEDIUM;
        }

        if (category == ComplaintCategory.HOUSEKEEPING || category == ComplaintCategory.PARKING) {
            return ComplaintPriority.LOW;
        }

        return ComplaintPriority.MEDIUM;
    }

    private int calculateConfidence(String text, ComplaintCategory category) {
        if (category == ComplaintCategory.OTHER) {
            return 60;
        }
        return 85;
    }
}

