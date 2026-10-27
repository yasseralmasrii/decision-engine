package com.yasser.decisionengine.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * What an agent wants to do. It only says WHAT, never the facts:
 * the engine looks facts up itself in the system of record.
 */
public record ProposedAction(
        String domain,              // "refund", "deploy", "ticket"
        String type,                // "issue_refund", "deploy", "route", "send_reply"
        Map<String, Object> params, // e.g. orderId + amount
        String requester            // who or what asked for it
) {
    public ProposedAction {
        domain = normalize(domain);
        type = normalize(type);
        // LinkedHashMap (not Map.copyOf) because JSON may contain null values.
        params = params == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(params));
    }

    private static String normalize(String s) {
        return s == null ? null : s.trim().toLowerCase(Locale.ROOT);
    }
}
