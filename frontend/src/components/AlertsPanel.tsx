import { useState } from 'react';
import { api } from '../api';
import type { Filters } from '../types';
import { useAsync } from '../useAsync';
import { fmtDMY, fmtINR } from '../utils';
import { Empty, ErrorBox, Loading, Section } from './Blocks';

export default function AlertsPanel({ filters }: { filters: Filters }) {
  const [version, setVersion] = useState(0);
  const [direction, setDirection] = useState<'ABOVE' | 'BELOW'>('ABOVE');
  const [threshold, setThreshold] = useState('');
  const [msg, setMsg] = useState<string>();
  const { data, loading, error } = useAsync(() => api.alerts(), [version]);

  const create = async () => {
    setMsg(undefined);
    try {
      await api.createAlert({ ...filters, market: filters.market || undefined, direction, threshold: Number(threshold) });
      setThreshold(''); setVersion((v) => v + 1);
    } catch (e) { setMsg(e instanceof Error ? e.message : 'Could not create alert.'); }
  };

  return (
    <Section title="Price alerts">
      <div className="controls">
        <span>{filters.commodity} · {filters.district}{filters.market ? ` · ${filters.market}` : ' · any market'}: alert when modal price is</span>
        <select value={direction} onChange={(e) => setDirection(e.target.value as 'ABOVE' | 'BELOW')}>
          <option value="ABOVE">above</option><option value="BELOW">below</option>
        </select>
        <input type="number" min="1" placeholder="₹ per quintal" value={threshold} onChange={(e) => setThreshold(e.target.value)} />
        <button className="primary" disabled={!(Number(threshold) > 0)} onClick={create}>Create alert</button>
      </div>
      {msg && <ErrorBox message={msg} />}
      {loading && <Loading />}
      {error && <ErrorBox message={error} />}
      {!loading && !error && data && data.length === 0 && <Empty>No alerts yet.</Empty>}
      {!loading && data && data.length > 0 && (
        <ul className="alerts">
          {data.map((a) => (
            <li key={a.id}>
              <div>
                <strong>{a.commodity}</strong> · {a.district} · {a.market ?? 'any market'} — {a.direction === 'ABOVE' ? 'above' : 'below'} {fmtINR(a.threshold)}
                <small>{a.lastObservedPrice === null ? 'No stored price to check yet.'
                  : `Latest stored: ${fmtINR(a.lastObservedPrice)} at ${a.lastObservedMarket} (${fmtDMY(a.lastObservedDate)})`}</small>
              </div>
              <span className={`badge ${a.triggered ? 'hit' : ''}`}>{a.triggered ? 'Condition met' : 'Not met'}</span>
              <button onClick={async () => { await api.deleteAlert(a.id); setVersion((v) => v + 1); }}>Delete</button>
            </li>
          ))}
        </ul>
      )}
      <p className="note">Alerts are checked against stored history after each daily sync. Notifications (email/SMS) are not part of v1.</p>
    </Section>
  );
}
