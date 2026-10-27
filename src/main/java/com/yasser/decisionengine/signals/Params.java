package com.yasser.decisionengine.signals;

import java.util.Map;

/** Reads values from an action's params safely (JSON numbers may arrive as strings). */
public final class Params {

    private Params() {}

    public static String str(Map<String, Object> p, String key) {
        Object v = p.get(key);
        return (v == null || v.toString().isBlank()) ? null : v.toString().trim();
    }

    /** Returns the fallback for missing, unparseable, NaN or infinite values. */
    public static double num(Map<String, Object> p, String key, double fallback) {
        Object v = p.get(key);
        double d;
        if (v instanceof Number n) {
            d = n.doubleValue();
        } else if (v != null) {
            try {
                d = Double.parseDouble(v.toString().trim());
            } catch (NumberFormatException e) {
                return fallback;
            }
        } else {
            return fallback;
        }
        return Double.isFinite(d) ? d : fallback;
    }

    public static double clamp(double x) {
        return Math.max(0.0, Math.min(1.0, x));
    }
}
