package com.yasser.decisionengine.signals;

import com.yasser.decisionengine.model.ProposedAction;
import com.yasser.decisionengine.model.Signals;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class TicketSignals implements SignalProvider {
    private static final List<String> SENSITIVE = List.of("lawyer", "legal", "chargeback", "lawsuit");
    private static final List<String> INJECTION = List.of("ignore previous", "system:", "you are now");
    @Override
    public String domain() { return "ticket"; }
    @Override
    public Signals compute(ProposedAction action) {
        var p = Params.of(action);
        List<String> evidence = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        String ticketId = Params.str(p, "ticketId");
        String text = Params.str(p, "text");
        boolean vip = "vip".equals(Params.str(p, "customerTier"));
        double confidence = Params.num(p, "classifierConfidence", 0.5);
        if (ticketId == null) missing.add("ticketId");
        if (text == null) missing.add("text");
        double risk = vip ? 0.4 : 0.1;
        String lower = text == null ? "" : text.toLowerCase(Locale.ROOT);
        if (SENSITIVE.stream().anyMatch(lower::contains)) {
            risk = Math.max(risk, 0.7);
            evidence.add("Legal or financial language detected");
        }
        if (INJECTION.stream().anyMatch(lower::contains)) {
            risk = 0.95;
            evidence.add("Possible prompt injection in ticket text");
        }
        if (vip) evidence.add("VIP customer");
        evidence.add("Classifier confidence " + confidence);
        boolean reversible = !"send_reply".equals(action.type()); 
        double cost = vip ? 200 : 20;
        return new Signals(confidence, Params.clamp(risk), reversible, evidence, missing, cost);
    }
}