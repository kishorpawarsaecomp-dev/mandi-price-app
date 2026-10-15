import { useMemo, useState } from 'react';
import { api } from '../api';
import type { Filters } from '../types';
import { useAsync } from '../useAsync';
import { fmtDMY, fmtINR } from '../utils';
import { Empty, ErrorBox, Loading, Section } from './Blocks';

export default function ComparisonPanel({ filters }: { filters: Filters }) {
  const [sort, setSort] = useState<'high' | 'low'>('high');
  const [variantKey, setVariantKey] = useState('');
  const [variety, grade] = variantKey ? variantKey.split('||') : [undefined, undefined];
  const { data, loading, error } = useAsync(
    () => api.compare({ state: filters.state, district: filters.district, commodity: filters.commodity, variety, grade }),
    [filters, variantKey],
  );
  const rows = useMemo(() => {
    const r = [...(data?.rows ?? [])];
    r.sort((a, b) => (sort === 'high' ? b.modalPrice - a.modalPrice : a.modalPrice - b.modalPrice));
    return r;
  }, [data, sort]);

  const top = rows[0];
  return (
    <Section title="Market comparison">
      <div className="controls">
        <label className="inline">Sort by
          <select value={sort} onChange={(e) => setSort(e.target.value as 'high' | 'low')}>
            <option value="high">Highest modal price</option><option value="low">Lowest modal price</option>
          </select>
        </label>
        {data && data.availableVariants.length > 1 && (
          <label className="inline">Variety / grade
            <select value={variantKey || `${data.variety ?? ''}||${data.grade ?? ''}`} onChange={(e) => setVariantKey(e.target.value)}>
              {data.availableVariants.map((v) => (
                <option key={`${v.variety}||${v.grade}`} value={`${v.variety}||${v.grade}`}>{v.variety || '—'} / {v.grade || '—'}</option>
              ))}
            </select>
          </label>
        )}
      </div>
      {loading && <Loading />}
      {error && <ErrorBox message={error} />}
      {!loading && !error && data && rows.length === 0 && <Empty>No markets reported this commodity in the selected district.</Empty>}
      {!loading && !error && data && rows.length > 0 && (
        <>
          <div className="table-wrap">
            <table>
              <thead><tr><th>Market</th><th className="num">Modal price</th><th>Arrival date</th></tr></thead>
              <tbody>
                {rows.map((r) => (
                  <tr key={r.market}>
                    <td>{r.market}</td><td className="num strong">{fmtINR(r.modalPrice)}</td>
                    <td>{fmtDMY(r.arrivalDate)}{data.latestDate && r.arrivalDate < data.latestDate && <small className="warn-text"> older than latest</small>}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {top && (
            <p className="note">
              {top.market} currently has the {sort === 'high' ? 'highest' : 'lowest'} reported modal price among the markets listed ({fmtINR(top.modalPrice)}).
              Your actual return depends on transport, quantity, quality, commission and local costs, so this is not a recommendation of where to sell.
            </p>
          )}
          <p className="source">Source: {data.source} · variety "{data.variety || '—'}", grade "{data.grade || '—'}" (varieties/grades are never mixed)</p>
        </>
      )}
    </Section>
  );
}
