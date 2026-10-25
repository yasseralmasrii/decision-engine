package com.yasser.decisionengine.signals;

import com.yasser.decisionengine.model.ProposedAction;
import java.util.Map;

final class Params {
    private Params() {}
    static Map<String, Object> of(ProposedAction action) {
        return action.params() == null ? Map.of() : action.params();
    }
    static String str(Map<String, Object> p, String key) {
        Object v = p.get(key);
        return (v == null || v.toString().isBlank()) ? null : v.toString();
    }
    static double num(Map<String, Object> p, String key, double fallback) {
        Object v = p.get(key);
        if (v instanceof Number n) return n.doubleValue();
        if (v != null) {
            try { return Double.parseDouble(v.toString()); }
            catch (NumberFormatException ignored) { }
        }
        return fallback;
    }
    static Boolean bool(Map<String, Object> p, String key) {
        Object v = p.get(key);
        if (v instanceof Boolean b) return b;
        return v == null ? null : Boolean.parseBoolean(v.toString());
    }
    static double clamp(double x) {
        return Math.max(0.0, Math.min(1.0, x));
    }
}