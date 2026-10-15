import type { Alert, ComparisonResponse, HistoryResponse, OptionsResponse, PriceResponse, PriceType } from './types';

const BASE = (import.meta.env.VITE_API_BASE as string | undefined) ?? '';

type Params = Record<string, string | number | string[] | undefined | null>;

export function qs(p: Params): string {
  const u = new URLSearchParams();
  for (const [k, v] of Object.entries(p)) {
    if (v === undefined || v === null || v === '') continue;
    if (Array.isArray(v)) v.forEach((x) => u.append(k, x));
    else u.append(k, String(v));
  }
  return u.toString();
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let res: Response;
  try {
    res = await fetch(BASE + path, init);
  } catch {
    throw new Error('Cannot reach the server. Please check your connection and try again.');
  }
  if (!res.ok) {
    let msg = 'Something went wrong. Please try again.';
    try {
      const body = await res.json();
      if (body?.message) msg = body.message;
    } catch { /* keep generic message */ }
    throw new Error(msg);
  }
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

/** empty variety/grade must be sent as "-" so the backend can tell "empty" from "not chosen" */
const part = (s: string | undefined) => (s === undefined ? undefined : s === '' ? '-' : s);

export const api = {
  states: () => request<string[]>('/api/mandi/states'),
  options: (state: string, district?: string, commodity?: string) =>
    request<OptionsResponse>(`/api/mandi/options?${qs({ state, district, commodity })}`),
  prices: (p: { state: string; district?: string; commodity?: string; market?: string; limit: number; offset: number }) =>
    request<PriceResponse>(`/api/mandi/prices?${qs(p)}`),
  history: (p: {
    state: string; district: string; commodity: string; market?: string; markets?: string[];
    variety?: string; grade?: string; from: string; to: string; priceType: PriceType;
  }) => request<HistoryResponse>(`/api/mandi/history?${qs({ ...p, variety: part(p.variety), grade: part(p.grade) })}`),
  compare: (p: { state: string; district: string; commodity: string; variety?: string; grade?: string }) =>
    request<ComparisonResponse>(`/api/mandi/compare?${qs({ ...p, variety: part(p.variety), grade: part(p.grade) })}`),
  alerts: () => request<Alert[]>('/api/alerts'),
  createAlert: (a: { state: string; district: string; commodity: string; market?: string; direction: string; threshold: number }) =>
    request<Alert>('/api/alerts', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(a) }),
  deleteAlert: (id: number) => request<void>(`/api/alerts/${id}`, { method: 'DELETE' }),
};
