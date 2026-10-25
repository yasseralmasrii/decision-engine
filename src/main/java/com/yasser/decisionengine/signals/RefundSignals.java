package com.yasser.decisionengine.signals;

import com.yasser.decisionengine.model.ProposedAction;
import com.yasser.decisionengine.model.Signals;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class RefundSignals implements SignalProvider {
    @Override
    public String domain() { return "refund"; }
    @Override
    public Signals compute(ProposedAction action) {
        var p = Params.of(action);
        List<String> evidence = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        String orderId = Params.str(p, "orderId");
        double amount = Params.num(p, "amount", 0);
        double days = Params.num(p, "daysSincePurchase", 0);
        double priorRefunds = Params.num(p, "refundsLast90Days", 0);
        boolean storeCredit = "store_credit".equals(Params.str(p, "method"));
        if (orderId == null) missing.add("orderId");
        else evidence.add("Order " + orderId + " found");
        if (amount <= 0) missing.add("amount");
        double confidence = 0.9;
        if (days > 30) {
            confidence -= 0.2;
            evidence.add("Outside 30-day window (" + (int) days + " days)");
        } else {
            evidence.add("Within 30-day window");
        }
        if (priorRefunds >= 3) {
            confidence -= 0.3;
            evidence.add((int) priorRefunds + " refunds in the last 90 days");
        }
        double risk = Params.clamp(amount / 1000.0 + (priorRefunds >= 3 ? 0.3 : 0));
        evidence.add(storeCredit ? "Store credit can be revoked" : "Card refund cannot be pulled back");
        return new Signals(confidence, risk, storeCredit, evidence, missing, amount);
    }
}