/** Left rail: the docket of cases a visitor can open. */
import { html, mount } from "../html.js";
import { DOMAIN_NAME } from "../copy.js";

const DOMAIN_ORDER = ["refund", "deploy", "ticket"];

function caseLink({ kind, id, title, idLabel, meta, verdict, selected }) {
  const key = `${kind}:${id}`;
  return html`
    <li>
      <button type="button" class="case-link ${verdict ? `v-${verdict}` : ""}" data-case="${key}"
              aria-current="${key === selected ? "true" : "false"}">
        <span class="case-link__title">${title}</span>
        <span class="case-link__id">${idLabel ?? ""}</span>
        <span class="case-link__meta">${verdict ? html`<span class="dot"></span>` : ""}<span>${meta}</span></span>
      </button>
    </li>`;
}

export function renderInbox(element, { tickets, scenarios, selected }) {
  const byDomain = new Map();
  for (const s of scenarios) {
    const domain = DOMAIN_ORDER.includes(s.action.domain) ? s.action.domain : "other";
    if (!byDomain.has(domain)) byDomain.set(domain, []);
    byDomain.get(domain).push(s);
  }

  mount(element, html`
    <section class="inbox__group" aria-labelledby="inbox-agent">
      <h2 class="inbox__title" id="inbox-agent">AI agent on live tickets</h2>
      <p class="inbox__hint">The agent reads the ticket and proposes. Try T-102.</p>
      <ul class="inbox__list">
        ${tickets.map((t) => caseLink({
          kind: "ticket", id: t.id, title: t.subject, idLabel: t.id, meta: t.text, selected,
        }))}
      </ul>
    </section>

    <section class="inbox__group" aria-labelledby="inbox-scenarios">
      <h2 class="inbox__title" id="inbox-scenarios">Scenario library</h2>
      <p class="inbox__hint">Curated cases covering every ruling in every domain.</p>
      ${[...byDomain.entries()].map(([domain, items]) => html`
        <h3 class="inbox__sub">${DOMAIN_NAME[domain] ?? "Unknown domains"}</h3>
        <ul class="inbox__list">
          ${items.map((s) => caseLink({
            kind: "scenario", id: s.id, title: s.title, meta: `Expect ${s.expected.toLowerCase()}`,
            verdict: s.expected, selected,
          }))}
        </ul>`)}
    </section>

    <section class="inbox__group" aria-labelledby="inbox-draft">
      <h2 class="inbox__title" id="inbox-draft">Your own case</h2>
      <ul class="inbox__list">
        ${caseLink({ kind: "draft", id: "new", title: "Draft an action", meta: "Write the JSON an agent would send", selected })}
      </ul>
    </section>`);
}

/** One delegated listener; survives re-renders. */
export function bindInbox(element, onOpen) {
  element.addEventListener("click", (event) => {
    const button = event.target.closest("[data-case]");
    if (!button) return;
    const [kind, ...rest] = button.dataset.case.split(":");
    onOpen(kind, rest.join(":"));
  });
}

export function markSelected(element, key) {
  element.querySelectorAll("[data-case]").forEach((b) => {
    b.setAttribute("aria-current", b.dataset.case === key ? "true" : "false");
  });
}
