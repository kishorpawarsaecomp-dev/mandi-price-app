import { render, screen } from '@testing-library/react';
import { vi } from 'vitest';
import HistoryPanel from '../components/HistoryPanel';

vi.mock('../api', () => ({ api: { history: vi.fn() } }));
import { api } from '../api';

const filters = { state: 'Maharashtra', district: 'Latur', commodity: 'Tur', market: '' };
const base = {
  state: 'Maharashtra', district: 'Latur', commodity: 'Tur', variety: 'Other', grade: 'FAQ', priceType: 'modal',
  period: { from: '2026-09-16', to: '2026-10-15' }, aggregationNote: 'Showing variety "Other", grade "FAQ".',
  availableMarkets: [{ market: 'A', records: 3 }], selectedMarkets: ['A'], availableVariants: [{ variety: 'Other', grade: 'FAQ' }],
  source: 'data.gov.in', disclaimer: 'Historical trend indicator based on reported prices only. It is not a forecast or a guarantee of future prices.',
};

describe('HistoryPanel', () => {
  it('renders chart, stats, coverage warning and disclaimer', async () => {
    (api.history as any).mockResolvedValue({
      ...base,
      records: [{ date: '2026-10-13', market: 'A', price: 100 }, { date: '2026-10-14', market: 'A', price: 110 }],
      stats: [{ market: 'A', dataPoints: 2, current: 110, currentDate: '2026-10-14', high: 110, low: 100, average: 105, changePercent: 10, trend: 'INSUFFICIENT_DATA' }],
      coverage: { firstRecordDate: '2026-10-13', lastRecordDate: '2026-10-14', daysWithData: 2, requestedDays: 30, sufficient: false, message: 'Not enough history yet for a 30-day view' },
    });
    render(<HistoryPanel filters={filters} />);
    expect(await screen.findByTestId('history-chart')).toBeInTheDocument();
    expect(screen.getByText(/Not enough history yet/)).toBeInTheDocument();
    expect(screen.getByText('+10%')).toBeInTheDocument();
    expect(screen.getByText(/not a forecast/i)).toBeInTheDocument();
  });

  it('shows an empty state when nothing is stored', async () => {
    (api.history as any).mockResolvedValue({
      ...base, records: [], stats: [], availableMarkets: [], selectedMarkets: [],
      coverage: { firstRecordDate: null, lastRecordDate: null, daysWithData: 0, requestedDays: 30, sufficient: false, message: 'No history stored for this selection yet.' },
    });
    render(<HistoryPanel filters={filters} />);
    expect(await screen.findByText(/No stored price records/)).toBeInTheDocument();
  });

  it('shows an API error', async () => {
    (api.history as any).mockRejectedValue(new Error('Mandi data is temporarily unavailable. Please try again later.'));
    render(<HistoryPanel filters={filters} />);
    expect(await screen.findByRole('alert')).toBeInTheDocument();
  });
});
