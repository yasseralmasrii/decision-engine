/** Draft your own action and file it with the engine. */
import { html, mount } from "../html.js";

export const TEMPLATES = {
  refund: {
    label: "Refund",
    tip: "File it twice: the engine remembers what it already approved. Or try an amount above the order total.",
    action: { domain: "refund", type: "issue_refund", requester: "you", params: { orderId: "A-1001", amount: 25 } },
  },
  deploy: {
    label: "Deploy",
    tip: "Try commit e4f5a6b (CI still running) or service payments (change freeze).",
    action: { domain: "deploy", type: "deploy", requester: "you", params: { service: "api", environment: "production", commit: "a1b2c3d" } },
  },
  ticket: {
    label: "Ticket",
    tip: "Try routing T-105 to billing: the engine's own classifier will disagree.",
    action: { domain: "ticket", type: "route", requester: "you", params: { ticketId: "T-105", queue: "shipping" } },
  },
  unknown: {
    label: "Something new",
    tip: "The engine has no rules for this domain. Watch what it does.",
    action: { domain: "payroll", type: "pay_salary", requester: "you", params: { employee: "E-7", amount: 5000 } },
  },
};

export function renderComposer(element, { template = "refund", text, error }) {
  const current = TEMPLATES[template] ?? TEMPLATES.refund;
  const body = text ?? JSON.stringify(current.action, null, 2);

  mount(element, html`
    <div class="sheet">
      <header class="case__head">
        <p class="case__kicker">Drafted case</p>
        <h1 class="case__title">File an action for a ruling</h1>
        <p class="case__meta"><span>Write the JSON an agent would send. The engine looks the facts up itself.</span></p>
      </header>
      <section class="case__sec">
        <span class="case__n" aria-hidden="true">1</span>
        <form class="case__body composer" data-composer novalidate>
          <h2 class="case__h">The action</h2>
          <div class="composer__templates" role="group" aria-label="Start from a template">
            ${Object.entries(TEMPLATES).map(([key, t]) => html`
              <button type="button" class="btn btn--quiet" data-template="${key}" aria-pressed="${key === template}">${t.label}</button>`)}
          </div>
          <label class="composer__tip" for="composer-json">${current.tip}</label>
          <textarea id="composer-json" name="action" spellcheck="false" autocomplete="off">${body}</textarea>
          ${error ? html`<p class="composer__error" role="alert">${error}</p>` : ""}
          <div class="composer__actions">
            <button type="submit" class="btn">File for a ruling</button>
          </div>
        </form>
      </section>
    </div>`);
}

/** Returns { action } or { error } with a message that says how to fix it. */
export function readComposer(form) {
  const text = form.querySelector("textarea").value;
  let action;
  try {
    action = JSON.parse(text);
  } catch (e) {
    return { text, error: `That isn't valid JSON yet: ${e.message}` };
  }
  if (!action || typeof action !== "object" || Array.isArray(action)) {
    return { text, error: "The action must be a JSON object with domain, type and params." };
  }
  if (!action.domain) return { text, error: 'Add a "domain" field, for example "refund".' };
  return { text, action };
}
