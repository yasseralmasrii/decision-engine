/**
 * Center pane: one ruling as a case file, in the order the engine works:
 * request, proposal, findings, rule cascade, ruling.
 */
import { html, mount } from "../html.js";
import { percent, money, dateTime } from "../format.js";
import { describeAction, GATE_QUESTION, VERDICT_MEANING } from "../copy.js";

const STEP_MS = 150;
const prefersReducedMotion = () => window.matchMedia("(prefers-reduced-motion: reduce)").matches;

/* ---------- Sections ---------- */

function section(n, title, body, lede) {
  return html`
    <section class="case__sec">
      <span class="case__n" aria-hidden="true">${n}</span>
      <div class="case__body">
        <h2 class="case__h">${title}</h2>
        ${lede ? html`<p class="case__lede">${lede}</p>` : ""}
        ${body}
      </div>
    </section>`;
}

function requestSection({ entry, ticket, scenario, origin }) {
  if (ticket) {
    return section(1, "The request", html`
      <blockquote class="quote">
        ${ticket.text}
        <footer>Customer ${ticket.customerId}, ticket ${ticket.id}</footer>
      </blockquote>`);
  }
  if (scenario) return section(1, "The request", html`<p class="prose">${scenario.story}</p>`);
  const who = origin === "draft" ? "you, from this page" : (entry.input.requester ?? "an unnamed caller");
  return section(1, "The request", html`<p class="prose">Submitted directly to the engine by ${who}.</p>`);
}

function proposalSection({ entry }) {
  const action = entry.input;
  const proposal = entry.proposal;
  return section(2, "The agent's proposal", html`
    <p class="proposal__action">${describeAction(action)}</p>
    <dl class="kv">
      <div><dt>Proposed by</dt><dd>${proposal ? proposal.proposer : action.requester ?? "unknown"}</dd></div>
      ${proposal ? html`
        <div><dt>Agent's reasoning</dt><dd>${proposal.rationale}</dd></div>
        <div><dt>Claimed confidence</dt>
          <dd class="claim"><s class="num">${percent(proposal.selfReportedConfidence)}</s>
            <em>Not admitted as evidence.</em> The engine never relies on an agent's opinion of itself.</dd></div>` : ""}
    </dl>
    <details class="raw">
      <summary>Show the raw action</summary>
      <pre>${JSON.stringify(action, null, 2)}</pre>
    </details>`);
}

function blockers(signals) {
  const rows = [
    ...signals.violations.map((text) => ({ kind: "Policy violation", verdict: "REFUSE", text })),
    ...signals.missingInfo.map((text) => ({ kind: "Missing", verdict: "ASK", text })),
    ...signals.pending.map((text) => ({ kind: "Waiting on", verdict: "DEFER", text })),
  ];
  if (!rows.length) return "";
  return html`
    <ul class="blockers">
      ${rows.map((r) => html`
        <li class="blocker v-${r.verdict}"><span class="blocker__kind">${r.kind}</span><span>${r.text}</span></li>`)}
    </ul>`;
}

function findingsSection({ entry }) {
  const s = entry.decision.signals;
  return section(3, "Findings of fact", html`
    <dl class="measures">
      <div><dt>Confidence</dt><dd class="num">${percent(s.confidence)}</dd>
        <div class="meter" aria-hidden="true"><i style="width:${percent(s.confidence)}"></i></div></div>
      <div><dt>Risk</dt><dd class="num">${percent(s.risk)}</dd>
        <div class="meter" aria-hidden="true"><i style="width:${percent(s.risk)}"></i></div></div>
      <div><dt>Can be undone</dt><dd>${s.reversible ? "Yes" : "No"}</dd></div>
      <div><dt>Cost if wrong</dt><dd class="num">${money(s.costOfErrorUsd)}</dd></div>
    </dl>
    ${blockers(s)}
    ${s.evidence.length ? html`<ul class="evidence">${s.evidence.map((e) => html`<li>${e}</li>`)}</ul>` : ""}`,
    "Looked up in the system of record. Nothing on this list comes from the agent.");
}

function cascadeSection({ entry }) {
  const trace = entry.decision.trace;
  const decidingIndex = trace.findIndex((c) => c.decided);
  return section(4, "Rule cascade", html`
    <ol class="cascade">
      ${trace.map((check, i) => {
        const state = [
          check.matched ? "is-yes" : "is-no",
          check.decided ? "is-deciding" : "",
          i > decidingIndex ? "is-after" : "",
        ].join(" ");
        const outcome = check.decided
          ? html`Decides: <strong>${check.verdict.toLowerCase()}</strong>`
          : check.matched ? "Also true" : "No";
        return html`
          <li class="gate v-${check.verdict} ${state}">
            <span class="gate__mark" aria-hidden="true">${i + 1}</span>
            <div>
              <p class="gate__q">${GATE_QUESTION[check.rule] ?? check.rule}</p>
              <p class="gate__d">${check.detail}</p>
            </div>
            <span class="gate__out">${outcome}</span>
          </li>`;
      })}
    </ol>`,
    "Checked in order. The first rule that applies decides; later rules are shown for context.");
}

function rulingSection({ entry }) {
  const d = entry.decision;
  return section(5, "Ruling", html`
    <div class="ruling v-${d.verdict}">
      <div class="stamp" data-stamp role="img" aria-label="Ruling: ${d.verdict.toLowerCase()}">${d.verdict}</div>
      <div class="ruling__text">
        <p class="ruling__reason">${d.reasoning}</p>
        <p class="ruling__order"><strong>Order.</strong> ${d.nextStep}</p>
        <p class="ruling__meaning">${VERDICT_MEANING[d.verdict]}</p>
      </div>
    </div>
    <details class="raw">
      <summary>Show the sealed record entry</summary>
      <dl class="seal">
        <div><dt>Entry</dt><dd>${entry.id}</dd></div>
        <div><dt>Policy version</dt><dd>${d.policyVersion}</dd></div>
        <div><dt>Hash</dt><dd>${entry.hash}</dd></div>
        <div><dt>Previous hash</dt><dd>${entry.prevHash}</dd></div>
      </dl>
    </details>`);
}

/* ---------- Public API ---------- */

const KICKER = {
  ticket: (ctx) => `Agent case, ticket ${ctx.ticket.id}`,
  scenario: () => "Scenario",
  draft: () => "Drafted case",
  record: () => "From the decision record",
};

export function renderCase(element, ctx) {
  cancelAnimation(element);
  const { entry, ticket, scenario, origin } = ctx;
  const title = ticket?.subject ?? scenario?.title ?? describeAction(entry.input);

  mount(element, html`
    <div class="sheet">
      <header class="case__head">
        <p class="case__kicker">${(KICKER[origin] ?? KICKER.record)(ctx)}</p>
        <h1 class="case__title">${title}</h1>
        <p class="case__meta">
          <span>Filed <strong class="num">${dateTime(entry.timestamp)}</strong></span>
          <span>Domain <strong>${entry.input.domain}</strong></span>
          <span>Ruled <strong>${entry.decision.verdict.toLowerCase()}</strong> by rule <strong>${entry.decision.ruleFired}</strong></span>
        </p>
        ${origin === "draft" ? html`
          <div class="case__head-actions"><button type="button" class="btn btn--quiet" data-action="compose">Edit and re-file</button></div>` : ""}
      </header>
      ${requestSection(ctx)}
      ${proposalSection(ctx)}
      ${findingsSection(ctx)}
      ${cascadeSection(ctx)}
      ${rulingSection(ctx)}
    </div>`);

  if (ctx.animate) playRuling(element);
}

export function renderPlaceholder(element, { title, body, working = false }) {
  cancelAnimation(element);
  mount(element, html`
    <div class="sheet">
      <div class="placeholder ${working ? "working" : ""}">
        <strong>${title}</strong>
        ${body ? html`<p>${body}</p>` : ""}
      </div>
    </div>`);
}

/**
 * The signature moment: gates light up one by one, stop at the deciding rule,
 * then the verdict is stamped. Starts when the cascade scrolls into view.
 */
function playRuling(element) {
  if (prefersReducedMotion()) return;
  const gates = [...element.querySelectorAll(".gate")];
  const stamp = element.querySelector("[data-stamp]");
  const cascade = element.querySelector(".cascade");
  if (!gates.length || !stamp || !cascade) return;

  gates.forEach((g) => g.classList.add("is-pending"));
  stamp.classList.add("is-pending");

  const timers = [];
  const start = () => {
    gates.forEach((gate, i) => timers.push(setTimeout(() => gate.classList.remove("is-pending"), STEP_MS * (i + 1))));
    timers.push(setTimeout(() => {
      stamp.classList.remove("is-pending");
      stamp.classList.add("is-stamped");
    }, STEP_MS * (gates.length + 1) + 80));
  };

  const observer = new IntersectionObserver((entries) => {
    if (entries.some((e) => e.isIntersecting)) {
      observer.disconnect();
      start();
    }
  }, { threshold: 0.35 });
  observer.observe(cascade);

  element._ruling = { timers, observer };
}

function cancelAnimation(element) {
  const state = element._ruling;
  if (!state) return;
  state.timers.forEach(clearTimeout);
  state.observer.disconnect();
  element._ruling = null;
}
