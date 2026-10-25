package com.yasser.decisionengine.signals;

import com.yasser.decisionengine.model.Signals;
import java.util.ArrayList;
import java.util.List;

/**
 * Collects measurements step by step, then freezes them into an immutable
 * Signals.
 */
public final class SignalsBuilder {

    private double confidence = 0.5;
    private double risk = 0.0;
    private boolean reversible = true;
    private double costOfErrorUsd = 0.0;
    private String approver = "human-reviewer";
    private final List<String> evidence = new ArrayList<>();
    private final List<String> missing = new ArrayList<>();
    private final List<String> pending = new ArrayList<>();
    private final List<String> violations = new ArrayList<>();

    public SignalsBuilder confidence(double v) {
        confidence = Params.clamp(v);
        return this;
    }

    public SignalsBuilder adjustConfidence(double delta) {
        return confidence(confidence + delta);
    }

    public SignalsBuilder risk(double v) {
        risk = Params.clamp(v);
        return this;
    }

    public SignalsBuilder raiseRiskTo(double v) {
        return risk(Math.max(risk, v));
    }

    public SignalsBuilder reversible(boolean v) {
        reversible = v;
        return this;
    }

    public SignalsBuilder costOfErrorUsd(double v) {
        costOfErrorUsd = Math.max(0, v);
        return this;
    }

    public SignalsBuilder approver(String v) {
        approver = v;
        return this;
    }

    public SignalsBuilder evidence(String v) {
        evidence.add(v);
        return this;
    }

    public SignalsBuilder missing(String v) {
        missing.add(v);
        return this;
    }

    public SignalsBuilder pending(String v) {
        pending.add(v);
        return this;
    }

    public SignalsBuilder violation(String v) {
        violations.add(v);
        return this;
    }

    public double risk() {
        return risk;
    }

    public Signals build() {
        return new Signals(confidence, risk, reversible, costOfErrorUsd,
                evidence, missing, pending, violations, approver);
    }
}
