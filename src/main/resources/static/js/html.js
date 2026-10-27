/**
 * Tiny, safe templating: html`...` escapes every interpolated value unless it
 * is itself the result of html`` (or explicitly wrapped with raw()).
 * This keeps ticket text, which is untrusted, from ever becoming markup.
 */

const ESCAPES = { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" };

class SafeHtml {
  constructor(value) { this.value = value; }
  toString() { return this.value; }
}

export const escapeHtml = (value) => String(value ?? "").replace(/[&<>"']/g, (c) => ESCAPES[c]);

export const raw = (value) => new SafeHtml(String(value));

function render(value) {
  if (value instanceof SafeHtml) return value.value;
  if (Array.isArray(value)) return value.map(render).join("");
  if (value === null || value === undefined || value === false) return "";
  return escapeHtml(value);
}

export function html(strings, ...values) {
  let out = "";
  strings.forEach((chunk, i) => {
    out += chunk;
    if (i < values.length) out += render(values[i]);
  });
  return new SafeHtml(out);
}

export function mount(element, view) {
  element.innerHTML = render(view);
}
