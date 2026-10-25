package com.yasser.decisionengine.model;

/** The engine's answer. */
public record Decision(
        Verdict verdict,
        Signals signals,
        String ruleFired,  // which policy rule decided it
        String reasoning   // a human-readable explanation
) {}