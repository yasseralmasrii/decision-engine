package com.yasser.decisionengine.ledger;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * The engine's memory of refunds it has already approved for automatic
 * execution.
 *
 * Without it, every request is judged in isolation, which is exactly how the
 * failure test broke the first version: the same $25 refund on a $45 order was
 * approved ten times, and a large refund split into small ones slipped under
 * every threshold (see docs/failure-test.md).
 *
 * Intentionally out of scope: persistence. The ledger lives in memory, so a
 * restart forgets past approvals. Production would write to the order system.
 */
@Component
public class RefundLedger {

    public record Approval(String orderId, String customerId, double amount, Instant at) {
    }

    private final List<Approval> approvals = new ArrayList<>();

    public synchronized void record(Approval approval) {
        approvals.add(approval);
    }

    public synchronized double approvedForOrder(String orderId) {
        return approvals.stream()
                .filter(a -> a.orderId().equals(orderId))
                .mapToDouble(Approval::amount)
                .sum();
    }

    public synchronized double approvedForCustomerSince(String customerId, Instant since) {
        return approvals.stream()
                .filter(a -> a.customerId().equals(customerId) && !a.at().isBefore(since))
                .mapToDouble(Approval::amount)
                .sum();
    }

    public synchronized List<Approval> all() {
        return List.copyOf(approvals);
    }
}
