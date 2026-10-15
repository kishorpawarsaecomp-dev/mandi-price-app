import { api } from '../api';
import type { Filters } from '../types';
import { useAsync } from '../useAsync';
import { fmtDMY, fmtINR } from '../utils';
import { Empty, ErrorBox, Loading, Section } from './Blocks';
import { useEffect, useState } from 'react';

const PAGE = 20;

export default function PriceTable({ filters }: { filters: Filters }) {
  const [offset, setOffset] = useState(0);
  useEffect(() => setOffset(0), [filters]);
  const { data, loading, error } = useAsync(
    () => api.prices({ state: filters.state, district: filters.district, commodity: filters.commodity, market: filters.market, limit: PAGE, offset }),
    [filters, offset],
  );

  return (
    <Section title="Current market prices">
      {loading && <Loading />}
      {error && <ErrorBox message={error} />}
      {!loading && !error && data && data.records.length === 0 && (
        <Empty>No mandi price records found for the selected filters.</Empty>
      )}
      {!loading && !error && data && data.records.length > 0 && (
        <>
          <div className="table-wrap">
            <table>
              <thead>
                <tr><th>Market</th><th>Commodity</th><th>Variety / Grade</th><th className="num">Min</th><th className="num">Max</th><th className="num">Modal</th><th>Arrival date</th></tr>
              </thead>
              <tbody>
                {data.records.map((r, i) => (
                  <tr key={`${r.market}-${r.variety}-${r.grade}-${r.arrivalDate}-${i}`}>
                    <td>{r.market}<small>{r.district}</small></td>
                    <td>{r.commodity}</td>
                    <td>{r.variety || '—'} / {r.grade || '—'}</td>
                    <td className="num">{fmtINR(r.minPrice)}</td>
                    <td className="num">{fmtINR(r.maxPrice)}</td>
                    <td className="num strong">{fmtINR(r.modalPrice)}</td>
                    <td>{fmtDMY(r.arrivalDate)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="pager">
            <button disabled={offset === 0} onClick={() => setOffset(Math.max(0, offset - PAGE))}>← Previous</button>
            <span>Showing {offset + 1}–{offset + data.records.length} of {data.total}</span>
            <button disabled={offset + PAGE >= data.total} onClick={() => setOffset(offset + PAGE)}>Next →</button>
          </div>
          <p className="source">Source: {data.source} · prices are per quintal as reported · check the arrival date, older rows are not today's price</p>
        </>
      )}
    </Section>
  );
}
