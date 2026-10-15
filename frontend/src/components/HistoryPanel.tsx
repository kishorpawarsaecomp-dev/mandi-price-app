import { useMemo, useState } from 'react';
import { CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { api } from '../api';
import type { Filters, PriceType } from '../types';
import { useAsync } from '../useAsync';
import { buildChartRows, fmtINR, fmtLong, fmtShort, PERIODS, rangeFor, TREND_LABEL, type Period } from '../utils';
import { Empty, ErrorBox, Loading, Section } from './Blocks';

const COLORS = ['#1b7f4b', '#c2410c', '#1d4ed8', '#a21caf', '#0f766e', '#b45309', '#be123c', '#4d7c0f'];
const MAX_MARKETS = 8;

export default function HistoryPanel({ filters }: { filters: Filters }) {
  const [period, setPeriod] = useState<Period>('30D');
  const [priceType, setPriceType] = useState<PriceType>('modal');
  const [markets, setMarkets] = useState<string[] | null>(null);   // null = backend default (top markets)
  const [variantKey, setVariantKey] = useState('');                  // "variety||grade"
  const [focus, setFocus] = useState('');
  const range = useMemo(() => rangeFor(period), [period]);
  const [variety, grade] = variantKey ? variantKey.split('||') : [undefined, undefined];

  const { data, loading, error } = useAsync(
    () => api.history({
      state: filters.state, district: filters.district, commodity: filters.commodity,
      market: filters.market || undefined, markets: markets ?? undefined,
      variety, grade, from: range.from, to: range.to, priceType,
    }),
    [filters, period, priceType, markets?.join('\u0000'), variantKey],
  );

  const selected = markets ?? data?.selectedMarkets ?? [];
  const rows = useMemo(
    () => (data ? buildChartRows(data.records, data.selectedMarkets, data.period.from, data.period.to) : []),
    [data],
  );
  const stat = data?.stats.find((s) => s.market === focus) ?? data?.stats[0];

  const toggle = (m: string) => {
    const next = selected.includes(m) ? selected.filter((x) => x !== m) : [...selected, m];
    if (next.length === 0 || next.length > MAX_MARKETS) return;
    setMarkets(next);
  };

  return (
    <Section title="Price trend">
      <div className="controls">
        <div className="seg" role="group" aria-label="Period">
          {(Object.keys(PERIODS) as Period[]).map((p) => (
            <button key={p} className={p === period ? 'on' : ''} onClick={() => setPeriod(p)}>{p}</button>
          ))}
        </div>
        <label className="inline">Price
          <select value={priceType} onChange={(e) => setPriceType(e.target.value as PriceType)}>
            <option value="modal">Modal price</option><option value="min">Minimum price</option><option value="max">Maximum price</option>
          </select>
        </label>
        {data && data.availableVariants.length > 1 && (
          <label className="inline">Variety / grade
            <select value={variantKey || `${data.variety}||${data.grade}`}
                    onChange={(e) => { setVariantKey(e.target.value); setMarkets(null); }}>
              {data.availableVariants.map((v) => (
                <option key={`${v.variety}||${v.grade}`} value={`${v.variety}||${v.grade}`}>{v.variety || '—'} / {v.grade || '—'}</option>
              ))}
            </select>
          </label>
        )}
      </div>

      {loading && <Loading text="Loading history…" />}
      {error && <ErrorBox message={error} />}

      {!loading && !error && data && (
        <>
          <p className="note">{data.aggregationNote}</p>
          <div className={`note ${data.coverage.sufficient ? '' : 'warn'}`}>{data.coverage.message}</div>

          {data.records.length === 0 ? (
            <Empty>No stored price records for this selection and period.</Empty>
          ) : (
            <>
              {!filters.market && data.availableMarkets.length > 1 && (
                <div className="chips" aria-label="Markets shown on chart">
                  <span>Markets on chart (max {MAX_MARKETS}):</span>
                  {data.availableMarkets.map((m) => (
                    <label key={m.market} className={`chip ${selected.includes(m.market) ? 'on' : ''}`}>
                      <input type="checkbox" checked={selected.includes(m.market)} onChange={() => toggle(m.market)} />
                      {m.market} <small>({m.records}d)</small>
                    </label>
                  ))}
                </div>
              )}

              <div className="chart" data-testid="history-chart">
                <ResponsiveContainer width="100%" height={320}>
                  <LineChart data={rows} margin={{ top: 8, right: 16, bottom: 8, left: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="date" tickFormatter={fmtShort} minTickGap={28} />
                    <YAxis tickFormatter={(v) => `₹${v}`} width={64} domain={['auto', 'auto']} />
                    <Tooltip labelFormatter={(l) => `Date: ${fmtLong(String(l))}`}
                             formatter={(v, name) => [fmtINR(Number(v)), `${String(name)} · ${data.priceType} price`]} />
                    <Legend />
                    {data.selectedMarkets.map((m, i) => (
                      <Line key={m} type="monotone" dataKey={m} name={m} stroke={COLORS[i % COLORS.length]}
                            strokeWidth={2} dot={{ r: 3 }} connectNulls={false} isAnimationActive={false} />
                    ))}
                  </LineChart>
                </ResponsiveContainer>
              </div>
              <p className="note">Gaps mean no record was reported for that day (e.g. market closed or data not published). They do not mean the price was ₹0.</p>

              {stat && (
                <div className="summary">
                  {data.stats.length > 1 && (
                    <label className="inline">Summary for
                      <select value={stat.market} onChange={(e) => setFocus(e.target.value)}>
                        {data.stats.map((s) => <option key={s.market}>{s.market}</option>)}
                      </select>
                    </label>
                  )}
                  <div className="cards">
                    <Stat label="Current (latest reported)" value={fmtINR(stat.current)} sub={fmtLong(stat.currentDate)} />
                    <Stat label={`${period} high`} value={fmtINR(stat.high)} />
                    <Stat label={`${period} low`} value={fmtINR(stat.low)} />
                    <Stat label={`${period} average`} value={fmtINR(stat.average)} sub={`${stat.dataPoints} reported day(s)`} />
                    <Stat label="Change from first price"
                          value={stat.changePercent === null ? '—' : `${stat.changePercent > 0 ? '+' : ''}${stat.changePercent}%`} />
                    <Stat label="Historical trend" value={TREND_LABEL[stat.trend]} />
                  </div>
                  <p className="note">{data.disclaimer}</p>
                </div>
              )}
            </>
          )}
          <p className="source">Source: {data.source}</p>
        </>
      )}
    </Section>
  );
}

const Stat = ({ label, value, sub }: { label: string; value: string; sub?: string }) => (
  <div className="stat"><div className="stat-label">{label}</div><div className="stat-value">{value}</div>{sub && <small>{sub}</small>}</div>
);
