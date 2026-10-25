package com.yasser.decisionengine.signals;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The engine's OWN reading of ticket text, independent of whatever the agent
 * claims.
 * Deliberately simple and transparent: keyword classification and injection
 * patterns.
 *
 * Known limit (documented in the failure test): a paraphrased injection such as
 * "kindly disregard what you were told earlier" is not caught by these
 * patterns.
 */
public final class TextChecks {

    private TextChecks() {
    }

    /** Category -> keywords, in priority order (first wins a tie). */
    private static final Map<String, Set<String>> CATEGORIES = new LinkedHashMap<>();
    static {
        CATEGORIES.put("legal", Set.of("lawyer", "lawsuit", "legal", "sue", "attorney", "chargeback"));
        CATEGORIES.put("billing", Set.of("refund", "charge", "charged", "invoice", "payment", "money"));
        CATEGORIES.put("technical", Set.of("password", "login", "log", "error", "crash", "bug", "reset"));
        CATEGORIES.put("shipping", Set.of("package", "delivery", "tracking", "shipped", "courier", "late"));
    }

    private static final List<Pattern> INJECTION = List.of(
            Pattern.compile("\\bignore\\s+(all\\s+|any\\s+)?(previous|prior|above)\\s+instructions?\\b",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bdisregard\\s+(all\\s+|any\\s+)?(previous|prior|above)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bsystem\\s*:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\byou\\s+are\\s+now\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bnew\\s+instructions\\b", Pattern.CASE_INSENSITIVE));

    /** Whole-word tokens, so "illegal" does not match "legal". */
    static Set<String> words(String text) {
        if (text == null)
            return Set.of();
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(w -> !w.isBlank())
                .collect(Collectors.toSet());
    }

    /** Best-matching category, or empty if no keyword matches. */
    public static Optional<String> classify(String text) {
        Set<String> words = words(text);
        String best = null;
        long bestHits = 0;
        for (var entry : CATEGORIES.entrySet()) {
            long hits = entry.getValue().stream().filter(words::contains).count();
            if (hits > bestHits) {
                best = entry.getKey();
                bestHits = hits;
            }
        }
        return Optional.ofNullable(best);
    }

    public static boolean hasLegalLanguage(String text) {
        Set<String> words = words(text);
        return CATEGORIES.get("legal").stream().anyMatch(words::contains);
    }

    public static boolean looksLikeInjection(String text) {
        return text != null && INJECTION.stream().anyMatch(p -> p.matcher(text).find());
    }
}
