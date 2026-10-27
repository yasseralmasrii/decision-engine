/**
 * Plain-language copy: turns machine names into sentences a stranger
 * understands in seconds. Kept in one place so wording stays consistent.
 */
import { money, titleCase } from "./format.js";

export const VERDICT_MEANING = {
  EXECUTE: "The action goes ahead without a person in the loop.",
  ASK: "The requester is asked for what is missing, then the case is re-filed.",
  DEFER: "Nothing happens yet. The case is re-checked once the evidence arrives.",
  ESCALATE: "A person with authority makes the call. The engine never acts alone here.",
  REFUSE: "The action is blocked. No level of confidence overrides a hard policy.",
};

/** Each rule, phrased as the question the engine asks. */
export const GATE_QUESTION = {
  "policy-violation": "Does a hard policy forbid it?",
  "missing-info": "Is information missing that only the requester has?",
  "pending-evidence": "Is evidence still on its way?",
  "low-confidence": "Is the engine less than 80% confident?",
  "high-stakes": "Are the stakes too high to act alone?",
  "safe-to-act": "Otherwise, act.",
};

export const DOMAIN_NAME = { refund: "Refunds", deploy: "Deploys", ticket: "Support tickets" };

/** One sentence describing what the agent wants to do. */
export function describeAction(action) {
  const p = action?.params ?? {};
  const key = `${action?.domain}.${action?.type}`;
  switch (key) {
    case "refund.issue_refund": {
      const amount = p.amount != null ? money(p.amount) : "an unstated amount";
      const order = p.orderId ? `order ${p.orderId}` : "an unstated order";
      const method = p.method ? ` as ${String(p.method).replace("_", " ")}` : "";
      return `Refund ${amount} on ${order}${method}`;
    }
    case "deploy.deploy":
      return `Deploy ${p.service ?? "an unstated service"} at commit ${p.commit ?? "?"} to ${p.environment ?? "an unstated environment"}`;
    case "ticket.route":
      return `Route ticket ${p.ticketId ?? "?"} to the ${p.queue ?? "unstated"} queue`;
    case "ticket.send_reply":
      return `Send a customer reply on ticket ${p.ticketId ?? "?"}`;
    case "ticket.close":
      return `Close ticket ${p.ticketId ?? "?"}`;
    default:
      return `${titleCase(action?.domain ?? "unknown")}: ${action?.type ?? "unknown action"}`;
  }
}
