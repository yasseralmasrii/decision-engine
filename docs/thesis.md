# Where decision layers go next: a two-year thesis

**Within two years, "what is this agent allowed to do?" will be answered by a decision layer outside the model, and that layer, not the model, will be where trust is won.**

**1. Model confidence becomes inadmissible.** Agents that act because the model says it is sure will look as naive as letting a defendant judge their own trial. Serious systems will ground every ruling in facts from systems of record and treat the model's self-assessment as testimony, not evidence. Asking a second model to judge the first is the same mistake with extra steps.

**2. The dangerous failures are sequences, not single actions.** Agents retry, loop and parallelize. My own engine was correct about every individual refund and still paid out $250 on a $45 order. Decision layers will become stateful by default: ledgers, budgets and velocity limits shared across all agents acting on the same customer, account or system.

**3. Policy becomes versioned, replayable code.** Every ruling will record the exact policy version that produced it. Before a policy ships, teams will replay months of real decisions against it. Auditors and regulators will ask "under which rules was this decided, and can you prove the record wasn't edited?", and hash-chained decision logs will be how teams answer.

**4. Escalation becomes the product.** The goal is not fewer humans; it is humans only at the margin. The winning layers will learn where to put that margin from human overrides, but thresholds will move only through reviewed policy changes, never silently.

**My bet:** by 2028, agent permissions will be a standard line item next to identity and access management. The models will be interchangeable. The decision layer, its policies, and an audit trail anyone can verify will be the moat.
