package com.yasser.decisionengine.signals;

import com.yasser.decisionengine.model.ProposedAction;
import com.yasser.decisionengine.model.Signals;

/**
 * Each domain knows how to measure its own actions against the system of
 * record.
 */
public interface SignalProvider {
    String domain();

    Signals compute(ProposedAction action);
}
