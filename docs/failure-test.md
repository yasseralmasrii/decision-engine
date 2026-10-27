# The deliberate failure test

## The attack

Can an agent get more money out than the policy allows, without ever breaking a single rule?

Version 1 of the engine judged every request **in isolation**. Each rule looked only at the action in front of it. So I attacked it with two cases designed to exploit that:

**1. Replay.** File the same refund again and again.

```
refund $25 on order A-1001 (order total $45)   x 10
```

**2. Salami.** Split a large refund into slices that are each "safe".

```
refund $50 on A-1010, A-1011, A-1012, A-1013   (customer C-7, four $60 orders)
```

## What happened (version 1)

| Attack | Rulings | Money out |
|---|---|---|
| Replay, 10 times | EXECUTE x 10 | **$250 on a $45 order** |
| Salami, 4 slices | EXECUTE x 4 | $200 with no human ever looking |

Every single ruling was "correct": each refund was small, inside the window, reversible enough, and below every threshold. The engine was right about each action and wrong about the situation. It had no memory.

This is the failure that matters most for agents, because agents retry. A loop, a crashed worker, or a prompt-injected instruction to "try again" turns one approval into many.

## The fix (version 2)

1. **A refund ledger.** When a refund is ruled `EXECUTE`, the engine records it. The refundable balance is now `order total - refunded in the order system - already approved by the engine`. The second $25 on A-1001 sees a $20 balance and is **refused** by a hard policy, with the reason in the trace:

   > Refund $25.00 exceeds refundable balance $20.00 on A-1001. *Evidence: $25.00 already approved by this engine on A-1001.*

2. **A per-customer velocity limit.** Automatic refunds for one customer are capped at **$150 per 24 hours**. Past that, risk rises to 0.7 and the action **escalates** to a person instead of executing. Escalate, not refuse: the fourth refund may be legitimate, it just should not be automatic.

3. **Decide versus preview.** Only real requests are remembered. The scenario library uses `preview()`, a dry run, so demos never spend real budget.

4. **Atomic decide.** Measuring and remembering happen in one synchronized step, so two identical refunds arriving at the same instant cannot both pass the balance check.

| Attack | Rulings now | Money out automatically |
|---|---|---|
| Replay, 10 times | EXECUTE, then REFUSE x 9 | $25 |
| Salami, 4 slices | EXECUTE x 3, then ESCALATE | $150, then a human decides |

All of this is enforced by [`FailureTest.java`](../src/test/java/com/yasser/decisionengine/engine/FailureTest.java). Try it on the live demo: **Draft an action**, keep the refund template, and file it twice.

## What still breaks

Being honest about the edges:

- **Restarts forget.** The ledger is in memory. After a restart, the replay works once more. Production would write approvals to the order system itself. (`knownLimit_freshLedgerForgetsPastApprovals` documents this.)
- **Many customers, one attacker.** The velocity limit is per customer. An attacker controlling many accounts can stay under it. That needs cross-account signals (device, payment instrument), which this demo does not model.
- **Paraphrased prompt injection.** The ticket checks catch common phrasings like "SYSTEM:" and "ignore previous instructions", but not "kindly disregard what you were told earlier" (`paraphrasedInjectionIsAKnownGap`). For refunds this does not matter, because the amount is checked against the order record. For ticket routing it does: a paraphrased injection can still get a ticket routed. The defense there is that routing is reversible and cheap.
- **A wrong system of record.** The engine trusts its facts. If the order system says an order is $6,000, a $6,000 refund looks fine. The decision layer is only as good as the data it checks against.

## What I learned

The hard part of "knowing when not to act" is not the single decision. It is the sequence. A decision layer without memory can be walked past one reasonable step at a time.
