import { useState } from 'react';
import { api } from './api';
import AlertsPanel from './components/AlertsPanel';
import Combobox from './components/Combobox';
import ComparisonPanel from './components/ComparisonPanel';
import HistoryPanel from './components/HistoryPanel';
import PriceTable from './components/PriceTable';
import type { Filters } from './types';
import { useAsync } from './useAsync';

const EMPTY: Filters = { state: '', district: '', commodity: '', market: '' };

export default function App() {
  const [draft, setDraft] = useState<Filters>(EMPTY);
  const [applied, setApplied] = useState<Filters | null>(null);

  const states = useAsync(() => api.states(), []);
  const districts = useAsync(draft.state ? () => api.options(draft.state) : null, [draft.state]);
  const commodities = useAsync(draft.state && draft.district ? () => api.options(draft.state, draft.district) : null, [draft.state, draft.district]);
  const markets = useAsync(draft.state && draft.district && draft.commodity ? () => api.options(draft.state, draft.district, draft.commodity) : null,
    [draft.state, draft.district, draft.commodity]);

  const districtOpts = districts.data?.districts ?? [];
  const commodityOpts = commodities.data?.commodities ?? [];
  const marketOpts = markets.data?.markets ?? [];

  const districtOk = !districts.data || districtOpts.includes(draft.district);
  const commodityOk = !draft.commodity || !commodities.data || commodityOpts.includes(draft.commodity);
  const canSearch = !!draft.state && !!draft.district && districtOk && commodityOk;

  const set = (patch: Partial<Filters>) => setDraft((d) => ({ ...d, ...patch }));
  const ready = applied && applied.district && applied.commodity;

  return (
    <div className="app">
      <header><h1>Mandi Market Price</h1><p>Daily mandi prices and price history from the official data.gov.in dataset</p></header>

      <section className="card filters">
        <label className="field"><span>State</span>
          <select value={draft.state} onChange={(e) => setDraft({ state: e.target.value, district: '', commodity: '', market: '' })}>
            <option value="">Select state</option>
            {(states.data ?? []).map((s) => <option key={s}>{s}</option>)}
          </select>
        </label>
        <Combobox id="district" label="District" value={draft.district} options={districtOpts} loading={districts.loading}
                  disabled={!draft.state} placeholder="Type or choose" onChange={(v) => set({ district: v, commodity: '', market: '' })} />
        <Combobox id="commodity" label="Commodity" value={draft.commodity} options={commodityOpts} loading={commodities.loading}
                  disabled={!draft.district} placeholder="Type e.g. Tur, then choose" onChange={(v) => set({ commodity: v, market: '' })} />
        <label className="field"><span>Market{markets.loading ? ' …' : ''}</span>
          <select value={draft.market} disabled={!draft.commodity} onChange={(e) => set({ market: e.target.value })}>
            <option value="">All markets</option>
            {marketOpts.map((m) => <option key={m}>{m}</option>)}
          </select>
        </label>
        <button className="primary" disabled={!canSearch} onClick={() => setApplied(draft)}>Search prices</button>
        {districts.error && <p className="hint error-text">{districts.error}</p>}
        {draft.commodity && !commodityOk && <p className="hint">Choose a commodity from the list — official names can differ from common names (e.g. "Tur" appears as "Red gram/Arhar/Tur(whole)").</p>}
        {draft.district && !districtOk && <p className="hint">Choose a district from the list.</p>}
        {(districts.data?.truncated || commodities.data?.truncated) && <p className="hint">Option lists were built from a partial scan of the source data.</p>}
      </section>

      {applied && <PriceTable key={JSON.stringify(applied)} filters={applied} />}
      {ready && (
        <>
          <HistoryPanel key={`h-${JSON.stringify(applied)}`} filters={applied} />
          <ComparisonPanel key={`c-${JSON.stringify(applied)}`} filters={applied} />
          <AlertsPanel key={`a-${JSON.stringify(applied)}`} filters={applied} />
        </>
      )}
      <footer>Source: data.gov.in (Agmarknet). Prices are as reported by markets; arrival dates may be older than today.</footer>
    </div>
  );
}
