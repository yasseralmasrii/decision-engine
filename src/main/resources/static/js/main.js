/**
 * Entry point: owns page state and wires views to the API.
 * Views are pure renderers; everything stateful happens here.
 */
import { api, onConnection } from "./api.js";
import { html, mount } from "./html.js";
import { renderInbox, bindInbox, markSelected } from "./views/inbox.js";
import { renderCase, renderPlaceholder } from "./views/caseFile.js";
import { renderRecord, bindRecord } from "./views/record.js";
import { renderComposer, readComposer } from "./views/composer.js";

const FEATURED_TICKET = "T-102";
const RECORD_LIMIT = 60;

const el = {
  inbox: document.getElementById("inbox"),
  case: document.getElementById("case"),
  record: document.getElementById("record"),
  banner: document.getElementById("banner"),
  policy: document.getElementById("status-policy"),
  agent: document.getElementById("status-agent"),
  chain: document.getElementById("status-chain"),
  footerVersion: document.getElementById("footer-version"),
};

const state = {
  tickets: [],
  scenarios: [],
  record: [],
  verification: null,
  selectedCase: `ticket:${FEATURED_TICKET}`,
  selectedEntry: null,
  composer: { template: "refund", text: null, error: null },
  busy: false,
};

/* ---------- Connection banner ---------- */

onConnection((type, error) => {
  if (type === "waking") {
    showBanner("DEFER", "The server is waking up. Free hosting pauses after 15 idle minutes, so the first request can take up to a minute.");
  } else if (type === "error") {
    showBanner("REFUSE", `Can't reach the engine: ${error?.message ?? "unknown error"}. Check your connection, then reload the page.`);
  } else {
    el.banner.hidden = true;
  }
});

function showBanner(verdict, message) {
  mount(el.banner, html`<div class="banner__inner v-${verdict}">${message}</div>`);
  el.banner.hidden = false;
}

/* ---------- Record and status ---------- */

async function refreshRecord() {
  const [entries, verification] = await Promise.all([api.record(RECORD_LIMIT), api.verify()]);
  state.record = entries;
  state.verification = verification;
  drawRecord();
  drawChainStatus();
}

function drawRecord() {
  renderRecord(el.record, { entries: state.record, selectedId: state.selectedEntry, verification: state.verification });
}

function drawChainStatus() {
  const v = state.verification;
  if (!v) return;
  el.chain.textContent = v.valid ? `sealed, ${v.entriesChecked} entries` : "seal broken";
  el.chain.className = v.valid ? "is-ok" : "is-bad";
}

/* ---------- Opening cases ---------- */

function select(kind, id) {
  state.selectedCase = `${kind}:${id}`;
  markSelected(el.inbox, state.selectedCase);
}

function showEntry(entry, ctx) {
  state.selectedEntry = entry.id;
  renderCase(el.case, { entry, animate: true, ...ctx });
  bringIntoView();
}

function bringIntoView() {
  el.case.focus({ preventScroll: true });
  if (window.matchMedia("(max-width: 860px)").matches) {
    el.case.scrollIntoView({ behavior: "smooth", block: "start" });
  }
}

async function run(task, working) {
  if (state.busy) return;
  state.busy = true;
  renderPlaceholder(el.case, { ...working, working: true });
  try {
    await task();
    await refreshRecord();
  } catch (error) {
    renderPlaceholder(el.case, { title: "That case could not be filed", body: error.message });
  } finally {
    state.busy = false;
  }
}

const openers = {
  ticket: (id) => run(async () => {
    const entry = await api.runAgent(id);
    showEntry(entry, { origin: "ticket", ticket: state.tickets.find((t) => t.id === id) });
  }, { title: `The agent is reading ${id}`, body: "Then the engine checks its proposal against the record." }),

  scenario: (id) => run(async () => {
    const entry = await api.runScenario(id);
    showEntry(entry, { origin: "scenario", scenario: state.scenarios.find((s) => s.id === id) });
  }, { title: "Filing the scenario", body: "The engine is gathering facts and checking its rules." }),

  draft: () => {
    renderComposer(el.case, state.composer);
    bringIntoView();
  },
};

function openCase(kind, id) {
  select(kind, id);
  openers[kind]?.(id);
}

function openEntry(entryId) {
  const entry = state.record.find((e) => e.id === entryId);
  if (!entry) return;
  const ticket = entry.proposal ? state.tickets.find((t) => t.id === entry.proposal.sourceTicketId) : null;
  showEntry(entry, { origin: "record", ticket });
  drawRecord();
}

/* ---------- Composer events (delegated on the case pane) ---------- */

el.case.addEventListener("click", (event) => {
  const template = event.target.closest("[data-template]");
  if (template) {
    state.composer = { template: template.dataset.template, text: null, error: null };
    renderComposer(el.case, state.composer);
    return;
  }
  if (event.target.closest('[data-action="compose"]')) openCase("draft", "new");
});

el.case.addEventListener("submit", (event) => {
  const form = event.target.closest("[data-composer]");
  if (!form) return;
  event.preventDefault();
  const result = readComposer(form);
  state.composer = { ...state.composer, text: result.text, error: result.error ?? null };
  if (result.error) {
    renderComposer(el.case, state.composer);
    return;
  }
  run(async () => {
    const entry = await api.decide(result.action);
    showEntry(entry, { origin: "draft" });
  }, { title: "Filing your action", body: "The engine is gathering facts and checking its rules." });
});

/* ---------- Boot ---------- */

bindInbox(el.inbox, openCase);
bindRecord(el.record, {
  onOpen: openEntry,
  onVerify: async () => {
    state.verification = await api.verify();
    drawRecord();
    drawChainStatus();
  },
});

(async function init() {
  renderPlaceholder(el.case, { title: "Opening the docket", working: true });
  try {
    const [policy, tickets, scenarios] = await Promise.all([api.policy(), api.tickets(), api.scenarios()]);
    state.tickets = tickets;
    state.scenarios = scenarios;
    el.policy.textContent = policy.version;
    el.agent.textContent = policy.agent;
    el.footerVersion.textContent = `Policy version ${policy.version}`;

    renderInbox(el.inbox, { tickets, scenarios, selected: state.selectedCase });
    await refreshRecord();

    // Open the featured case from the existing record, so loading the page files nothing new.
    const featured = state.record.find((e) => e.proposal?.sourceTicketId === FEATURED_TICKET);
    if (featured) {
      state.selectedEntry = featured.id;
      renderCase(el.case, {
        entry: featured, origin: "ticket", animate: true,
        ticket: tickets.find((t) => t.id === FEATURED_TICKET),
      });
      drawRecord();
    } else {
      renderPlaceholder(el.case, {
        title: "Open a case to see a ruling",
        body: "Pick a ticket for the AI agent, a scenario from the library, or draft your own action.",
      });
    }
  } catch (error) {
    renderPlaceholder(el.case, {
      title: "The engine isn't answering",
      body: `${error.message}. Reload the page in a moment; if it keeps failing, the server may be down.`,
    });
  }
})();
