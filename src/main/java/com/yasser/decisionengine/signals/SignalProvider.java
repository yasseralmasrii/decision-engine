package com.yasser.decisionengine.signals;

import com.yasser.decisionengine.model.ProposedAction;
import com.yasser.decisionengine.model.Signals;

/** Each domain knows how to measure its own actions against the system of record. */
public interface SignalProvider {

    String domain();

    Signals compute(ProposedAction action);

    /**
     * Called once an action has been approved for automatic execution, so the
     * domain can remember it (for example, refunds already paid out).
     * Not called for previews.
     */
    default void onExecuted(ProposedAction action) { }
}
