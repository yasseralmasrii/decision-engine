/**
 * API client. One place for HTTP, error shaping, and the "server is waking up"
 * signal (free hosting sleeps when idle; the first request can take ~1 minute).
 */

const WAKE_NOTICE_AFTER_MS = 2500;
const listeners = new Set();

/** Subscribe to connection events: "waking" | "ok" | "error". */
export function onConnection(listener) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}
const emit = (type, detail) => listeners.forEach((fn) => fn(type, detail));

export class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.status = status;
  }
}

async function request(path, { method = "GET", body } = {}) {
  const wakeTimer = setTimeout(() => emit("waking"), WAKE_NOTICE_AFTER_MS);
  try {
    const response = await fetch(path, {
      method,
      headers: body ? { "Content-Type": "application/json" } : undefined,
      body: body ? JSON.stringify(body) : undefined,
    });
    const data = await response.json().catch(() => null);
    if (!response.ok) {
      throw new ApiError(data?.error ?? `${method} ${path} returned ${response.status}`, response.status);
    }
    emit("ok");
    return data;
  } catch (error) {
    emit("error", error);
    throw error;
  } finally {
    clearTimeout(wakeTimer);
  }
}

const id = encodeURIComponent;

export const api = {
  policy: () => request("/api/policy"),
  tickets: () => request("/api/tickets"),
  scenarios: () => request("/api/scenarios"),
  record: (limit = 60) => request(`/api/audit?limit=${limit}`),
  verify: () => request("/api/audit/verify"),
  runAgent: (ticketId) => request(`/api/agent/tickets/${id(ticketId)}`, { method: "POST" }),
  runScenario: (scenarioId) => request(`/api/scenarios/${id(scenarioId)}/run`, { method: "POST" }),
  decide: (action) => request("/api/decide", { method: "POST", body: action }),
};
