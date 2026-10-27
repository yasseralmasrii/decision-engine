package com.yasser.decisionengine.model;

/**
 * One line of the decision trace: how a single rule evaluated.
 *
 * @param rule    rule name
 * @param verdict the verdict this rule would give
 * @param matched whether its condition held
 * @param decided whether this was the first match, i.e. the rule that decided
 * @param detail  the numbers behind the result, in plain words
 */
public record RuleCheck(String rule, Verdict verdict, boolean matched, boolean decided, String detail) {}
