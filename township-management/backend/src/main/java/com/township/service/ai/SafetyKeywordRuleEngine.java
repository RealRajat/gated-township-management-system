package com.township.service.ai;

import com.township.entity.ComplaintCategory;
import com.township.entity.ComplaintPriority;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic Safety Escalation Rule Engine.
 * Critical safety keywords strictly override AI classification and enforce CRITICAL priority.
 * AI downgrades are strictly prohibited by system specification.
 */
@Component
public class SafetyKeywordRuleEngine {

    // 1. Fire or Smoke
    private static final Pattern FIRE_PATTERN = Pattern.compile(
            "\\b(fire|smoke|burning|flame|flames|blaze|fire\\s*alarm|sparking\\s*meter|smoke\\s*detector|explosion)\\b",
            Pattern.CASE_INSENSITIVE
    );

    // 2. Gas Leak
    private static final Pattern GAS_PATTERN = Pattern.compile(
            "\\b(gas\\s*leak|gas\\s*smell|lpg\\s*leak|gas\\s*cylinder\\s*leak|cylinder\\s*leaking|gas\\s*pipe\\s*burst|piped\\s*gas\\s*leak)\\b",
            Pattern.CASE_INSENSITIVE
    );

    // 3. Lift Stuck or People Trapped
    private static final Pattern LIFT_STUCK_PATTERN = Pattern.compile(
            "\\b(lift\\s*stuck|elevator\\s*stuck|trapped\\s*in\\s*lift|trapped\\s*in\\s*elevator|lift\\s*trapped|lift\\s*fallen|lift\\s*fell|stuck\\s*in\\s*lift|lift\\s*door\\s*jammed\\s*with\\s*person)\\b",
            Pattern.CASE_INSENSITIVE
    );

    // 4. Electric Shock or Exposed Live Wires
    private static final Pattern ELECTRIC_PATTERN = Pattern.compile(
            "\\b(electric\\s*shock|electrocution|exposed\\s*wire|exposed\\s*live\\s*wire|live\\s*wire|sparking\\s*wire|short\\s*circuit\\s*with\\s*spark|sparking\\s*switchboard|current\\s*in\\s*tap|current\\s*in\\s*wall)\\b",
            Pattern.CASE_INSENSITIVE
    );

    public record SafetyMatch(
            boolean isSafetyEscalation,
            String groupName,
            String matchedKeyword,
            ComplaintCategory suggestedCategory,
            ComplaintPriority priority
    ) {
        public static SafetyMatch noMatch() {
            return new SafetyMatch(false, null, null, null, null);
        }
    }

    public SafetyMatch evaluate(String title, String description) {
        String text = ((title != null ? title : "") + " " + (description != null ? description : "")).toLowerCase(Locale.ENGLISH);

        // Check 1: Fire or smoke
        Matcher fireMatcher = FIRE_PATTERN.matcher(text);
        if (fireMatcher.find()) {
            return new SafetyMatch(
                    true,
                    "FIRE_OR_SMOKE",
                    fireMatcher.group(),
                    ComplaintCategory.SECURITY,
                    ComplaintPriority.CRITICAL
            );
        }

        // Check 2: Gas leak
        Matcher gasMatcher = GAS_PATTERN.matcher(text);
        if (gasMatcher.find()) {
            return new SafetyMatch(
                    true,
                    "GAS_LEAK",
                    gasMatcher.group(),
                    ComplaintCategory.PLUMBING,
                    ComplaintPriority.CRITICAL
            );
        }

        // Check 3: Lift stuck or people trapped
        Matcher liftMatcher = LIFT_STUCK_PATTERN.matcher(text);
        if (liftMatcher.find()) {
            return new SafetyMatch(
                    true,
                    "LIFT_STUCK_PEOPLE_TRAPPED",
                    liftMatcher.group(),
                    ComplaintCategory.LIFT,
                    ComplaintPriority.CRITICAL
            );
        }

        // Check 4: Electric shock or exposed live wires
        Matcher electricMatcher = ELECTRIC_PATTERN.matcher(text);
        if (electricMatcher.find()) {
            return new SafetyMatch(
                    true,
                    "ELECTRIC_SHOCK_EXPOSED_WIRES",
                    electricMatcher.group(),
                    ComplaintCategory.ELECTRICAL,
                    ComplaintPriority.CRITICAL
            );
        }

        return SafetyMatch.noMatch();
    }
}

