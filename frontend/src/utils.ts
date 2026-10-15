import type { HistoryRecord } from './types';

export const fmtINR = (n: number | null | undefined) =>
  n === null || n === undefined ? '—' : new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(n);

/** "2026-10-15" -> "15/10/2026" */
export const fmtDMY = (iso: string | null | undefined) => {
  if (!iso) return '—';
  const [y, m, d] = iso.split('-');
  return `${d}/${m}/${y}`;
};

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
/** "2026-10-15" -> "15 Oct 2026" */
export const fmtLong = (iso: string) => {
  const [y, m, d] = iso.split('-');
  return `${Number(d)} ${MONTHS[Number(m) - 1]} ${y}`;
};
export const fmtShort = (iso: string) => {
  const [, m, d] = iso.split('-');
  return `${Number(d)} ${MONTHS[Number(m) - 1]}`;
};

export const isoDate = (d: Date) => d.toISOString().slice(0, 10);

export const PERIODS = { '7D': 7, '30D': 30, '3M': 90, '6M': 180, '1Y': 365 } as const;
export type Period = keyof typeof PERIODS;

export function rangeFor(period: Period, today = new Date()): { from: string; to: string } {
  const to = new Date(Date.UTC(today.getFullYear(), today.getMonth(), today.getDate()));
  const from = new Date(to);
  from.setUTCDate(from.getUTCDate() - (PERIODS[period] - 1));
  return { from: isoDate(from), to: isoDate(to) };
}

/**
 * One row per CALENDAR day. Days with no reported record simply have no value for a market
 * (undefined) -> the chart shows a gap. Nothing is interpolated or carried forward.
 */
export function buildChartRows(records: HistoryRecord[], markets: string[], from: string, to: string) {
  const rows: Record<string, string | number | undefined>[] = [];
  const index = new Map<string, Record<string, string | number | undefined>>();
  const cur = new Date(`${from}T00:00:00Z`);
  const end = new Date(`${to}T00:00:00Z`);
  for (let i = 0; cur <= end && i < 4000; i++) {
    const row: Record<string, string | number | undefined> = { date: isoDate(cur) };
    rows.push(row);
    index.set(row.date as string, row);
    cur.setUTCDate(cur.getUTCDate() + 1);
  }
  for (const r of records) {
    if (!markets.includes(r.market)) continue;
    const row = index.get(r.date);
    if (row) row[r.market] = r.price;
  }
  return rows;
}

export const TREND_LABEL: Record<string, string> = {
  RISING: 'Rising', FALLING: 'Falling', STABLE: 'Relatively stable', INSUFFICIENT_DATA: 'Not enough data for a trend',
};
