package com.yasser.decisionengine.model;

import java.util.List;

/** What the engine measured about the action. */
public record Signals(
        double confidence,         // 0.0 to 1.0: how sure we are this is right
        double risk,               // 0.0 to 1.0: how bad it is if it goes wrong
        boolean reversible,        // can we undo it?
        List<String> evidence,     // facts that support acting
        List<String> missingInfo,  // facts we still need
        double costOfError         // estimated cost if wrong (e.g. in dollars)
) {}