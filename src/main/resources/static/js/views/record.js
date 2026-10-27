/** Right rail: the hash-chained decision record, newest first. */
import { html, mount } from "../html.js";
import { clock, shortHash } from "../format.js";
import { describeAction } from "../copy.js";

export function renderRecord(element, { entries, selectedId, verification }) {
  mount(element, html`
    <div class="record__head">
      <h2 class="record__title">Decision record</h2>
      <p class="record__lede">Every ruling is sealed with the hash of the one before it, so history cannot be edited or deleted unnoticed.</p>
      <div class="record__verify">
        <button type="button" class="btn btn--quiet" data-action="verify">Verify the seal</button>
        ${verification ? html`
          <span class="record__result ${verification.valid ? "is-ok" : "is-bad"}">
            ${verification.valid
              ? `Intact across ${verification.entriesChecked} entries`
              : `${verification.message} at ${shortHash(verification.brokenAt)}`}
          </span>` : ""}
      </div>
    </div>
    <ol class="chain">
      ${entries.map((e) => html`
        <li class="chain__node v-${e.decision.verdict}">
          <button type="button" class="chain__btn" data-entry="${e.id}" aria-current="${e.id === selectedId ? "true" : "false"}">
            <span class="chain__dot" aria-hidden="true"></span>
            <span class="chain__what">${describeAction(e.input)}</span>
            <span class="chain__meta">
              <span class="chain__verdict">${e.decision.verdict.toLowerCase()}</span>
              <span>${clock(e.timestamp)}</span>
              <span class="mono">${shortHash(e.hash)}</span>
            </span>
          </button>
        </li>`)}
    </ol>`);
}

export function bindRecord(element, { onOpen, onVerify }) {
  element.addEventListener("click", (event) => {
    const entry = event.target.closest("[data-entry]");
    if (entry) return onOpen(entry.dataset.entry);
    if (event.target.closest('[data-action="verify"]')) onVerify();
  });
}
