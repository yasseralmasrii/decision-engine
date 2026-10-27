package com.yasser.decisionengine.signals;

import com.yasser.decisionengine.data.Records.Customer;
import com.yasser.decisionengine.data.Records.Order;
import com.yasser.decisionengine.data.SystemOfRecord;
import com.yasser.decisionengine.model.ProposedAction;
import com.yasser.decisionengine.model.Signals;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Refund approval. The agent only supplies orderId + amount (+ optional
 * method);
 * everything else is looked up.
 */
@Component
public class RefundSignals implements SignalProvider {

    static final int REFUND_WINDOW_DAYS = 30;
    static final int FREQUENT_REFUNDER = 3;
    static final Set<String> METHODS = Set.of("card", "store_credit");

    private final SystemOfRecord records;

    public RefundSignals(SystemOfRecord records) {
        this.records = records;
    }

    @Override
    public String domain() {
        return "refund";
    }

    @Override
    public Signals compute(ProposedAction action) {
        var p = action.params();
        var s = new SignalsBuilder().approver("finance-approver").confidence(0.9);

        String orderId = Params.str(p, "orderId");
        double amount = Params.num(p, "amount", 0);
        s.costOfErrorUsd(amount).risk(amount / 1000.0);

        if (orderId == null)
            s.missing("orderId");
        if (amount <= 0)
            s.missing("refund amount");
        if (orderId == null)
            return s.build();

        Optional<Order> found = records.order(orderId);
        if (found.isEmpty()) {
            return s.missing("a valid orderId ('" + orderId + "' is not in the order system)").build();
        }
        Order order = found.get();
        Customer customer = records.customer(order.customerId()).orElse(null);
        s.evidence(String.format(Locale.ROOT, "Order %s: $%.2f paid by %s, status %s",
                order.id(), order.total(), order.paymentMethod(), order.status()));

        // --- Hard policies: these REFUSE, no matter how confident anyone is ---
        if (amount > order.refundable() + 0.005) {
            s.violation(String.format(Locale.ROOT, "Refund $%.2f exceeds refundable balance $%.2f on %s",
                    amount, order.refundable(), order.id()));
        }
        if (customer != null && customer.fraudHold()) {
            s.violation("Customer " + customer.id() + " is on fraud hold");
        }
        String method = Optional.ofNullable(Params.str(p, "method"))
                .map(m -> m.toLowerCase(Locale.ROOT)).orElse(order.paymentMethod());
        if (!METHODS.contains(method)) {
            s.violation("Unsupported refund method '" + method + "'");
        }

        // --- Time will resolve this: DEFER ---
        if ("RETURN_IN_TRANSIT".equals(order.status())) {
            s.pending("the returned item for " + order.id() + " to reach the warehouse");
        }

        // --- Soft signals: confidence and risk ---
        if (order.daysSincePurchase() > REFUND_WINDOW_DAYS) {
            s.adjustConfidence(-0.2).evidence("Outside " + REFUND_WINDOW_DAYS + "-day window ("
                    + order.daysSincePurchase() + " days since purchase)");
        } else {
            s.evidence("Within " + REFUND_WINDOW_DAYS + "-day window (" + order.daysSincePurchase() + " days)");
        }

        if (customer == null) {
            s.adjustConfidence(-0.3).evidence("Customer record not found");
        } else {
            if (customer.refundsLast90Days() >= FREQUENT_REFUNDER) {
                s.adjustConfidence(-0.3).risk(s.risk() + 0.3)
                        .evidence(customer.refundsLast90Days() + " refunds in the last 90 days (frequent refunder)");
            } else {
                s.evidence(customer.refundsLast90Days() + (customer.refundsLast90Days() == 1 ? " refund" : " refunds")
                        + " in the last 90 days");
            }
            if ("vip".equals(customer.tier()))
                s.evidence("VIP customer");
        }

        boolean storeCredit = "store_credit".equals(method);
        s.reversible(storeCredit)
                .evidence(storeCredit ? "Store credit can be revoked" : "Card refund cannot be pulled back");

        return s.build();
    }
}
