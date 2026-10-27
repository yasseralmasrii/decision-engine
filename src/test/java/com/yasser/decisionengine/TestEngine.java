package com.yasser.decisionengine;

import com.yasser.decisionengine.data.SystemOfRecord;
import com.yasser.decisionengine.engine.DecisionService;
import com.yasser.decisionengine.engine.PolicyEngine;
import com.yasser.decisionengine.ledger.RefundLedger;
import com.yasser.decisionengine.signals.DeploySignals;
import com.yasser.decisionengine.signals.RefundSignals;
import com.yasser.decisionengine.signals.TicketSignals;

import java.time.Clock;
import java.util.List;

/** Wires the engine by hand for fast tests, with a fresh ledger each time. */
public final class TestEngine {

    public static final SystemOfRecord RECORDS = new SystemOfRecord();

    private TestEngine() {
    }

    public static DecisionService decisions() {
        return decisions(new RefundLedger(), Clock.systemUTC());
    }

    public static DecisionService decisions(RefundLedger ledger, Clock clock) {
        return new DecisionService(
                List.of(new RefundSignals(RECORDS, ledger, clock), new DeploySignals(RECORDS),
                        new TicketSignals(RECORDS)),
                new PolicyEngine());
    }
}
