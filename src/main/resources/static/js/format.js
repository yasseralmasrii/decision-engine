/** Formatting helpers. Locale is pinned so numbers render the same for every judge. */

const LOCALE = "en-US";

export const percent = (x) => `${Math.round((Number(x) || 0) * 100)}%`;

export const money = (x) =>
  new Intl.NumberFormat(LOCALE, {
    style: "currency",
    currency: "USD",
    maximumFractionDigits: 2,
  }).format(Number(x) || 0);

/** Accepts ISO strings or epoch seconds (some JSON serializers emit either). */
export function toDate(timestamp) {
  return typeof timestamp === "number"
    ? new Date(timestamp * 1000)
    : new Date(timestamp);
}

const timeFormat = new Intl.DateTimeFormat("en-GB", {
  day: "numeric",
  month: "short",
  hour: "2-digit",
  minute: "2-digit",
  second: "2-digit",
});
export const dateTime = (timestamp) => timeFormat.format(toDate(timestamp));

const clockFormat = new Intl.DateTimeFormat("en-GB", {
  hour: "2-digit",
  minute: "2-digit",
  second: "2-digit",
});
export const clock = (timestamp) => clockFormat.format(toDate(timestamp));

export const shortHash = (hash) => (hash ? `${hash.slice(0, 8)}` : "");

export const titleCase = (s) =>
  s ? s.charAt(0).toUpperCase() + s.slice(1).toLowerCase() : "";
