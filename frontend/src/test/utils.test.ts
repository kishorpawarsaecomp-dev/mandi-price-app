import { buildChartRows, fmtDMY, fmtINR, rangeFor } from '../utils';

describe('utils', () => {
  it('formats dates and rupees', () => {
    expect(fmtDMY('2026-10-15')).toBe('15/10/2026');
    expect(fmtINR(9800)).toContain('9,800');
    expect(fmtINR(null)).toBe('—');
  });

  it('computes inclusive ranges', () => {
    const r = rangeFor('7D', new Date(2026, 9, 15));
    expect(r).toEqual({ from: '2026-10-09', to: '2026-10-15' });
  });

  it('keeps gaps for days without records (no interpolation)', () => {
    const rows = buildChartRows(
      [{ date: '2026-10-01', market: 'A', price: 10 }, { date: '2026-10-02', market: 'A', price: 11 },
       { date: '2026-10-04', market: 'A', price: 12 }, { date: '2026-10-05', market: 'A', price: 13 }],
      ['A'], '2026-10-01', '2026-10-05');
    expect(rows).toHaveLength(5);
    expect(rows[2].date).toBe('2026-10-03');
    expect(rows[2].A).toBeUndefined();
  });
});
