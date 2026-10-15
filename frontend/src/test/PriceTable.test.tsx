import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import PriceTable from '../components/PriceTable';

vi.mock('../api', () => ({ api: { prices: vi.fn() } }));
import { api } from '../api';

const filters = { state: 'Maharashtra', district: 'Latur', commodity: 'Tur', market: '' };
const rec = (i: number) => ({ state: 'Maharashtra', district: 'Latur', market: `Market ${i}`, commodity: 'Tur', variety: 'Other', grade: 'FAQ',
  arrivalDate: '2026-10-15', minPrice: 9400, maxPrice: 10200, modalPrice: 9800 });

describe('PriceTable', () => {
  afterEach(() => vi.clearAllMocks());

  it('shows loading then rows with arrival date and source', async () => {
    (api.prices as any).mockResolvedValue({ total: 1, limit: 20, offset: 0, records: [rec(1)], source: 'data.gov.in', fetchedAt: '' });
    render(<PriceTable filters={filters} />);
    expect(screen.getByRole('status')).toBeInTheDocument();
    expect(await screen.findByText('Market 1')).toBeInTheDocument();
    expect(screen.getByText('15/10/2026')).toBeInTheDocument();
    expect(screen.getByText(/Source: data.gov.in/)).toBeInTheDocument();
  });

  it('shows the empty state (no fake price)', async () => {
    (api.prices as any).mockResolvedValue({ total: 0, limit: 20, offset: 0, records: [], source: 'data.gov.in', fetchedAt: '' });
    render(<PriceTable filters={filters} />);
    expect(await screen.findByText('No mandi price records found for the selected filters.')).toBeInTheDocument();
    expect(screen.queryByText(/₹/)).not.toBeInTheDocument();
  });

  it('shows an error message', async () => {
    (api.prices as any).mockRejectedValue(new Error('Mandi data is temporarily unavailable. Please try again later.'));
    render(<PriceTable filters={filters} />);
    expect(await screen.findByRole('alert')).toHaveTextContent('temporarily unavailable');
  });

  it('paginates via offset', async () => {
    (api.prices as any).mockResolvedValue({ total: 45, limit: 20, offset: 0, records: Array.from({ length: 20 }, (_, i) => rec(i)), source: 'data.gov.in', fetchedAt: '' });
    render(<PriceTable filters={filters} />);
    await screen.findByText('Market 0');
    await userEvent.click(screen.getByText(/Next/));
    expect(api.prices).toHaveBeenLastCalledWith(expect.objectContaining({ offset: 20, limit: 20 }));
  });
});
