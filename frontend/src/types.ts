export interface Filters { state: string; district: string; commodity: string; market: string }

export interface MandiRecord {
  state: string; district: string; market: string; commodity: string; variety: string; grade: string;
  arrivalDate: string; minPrice: number | null; maxPrice: number | null; modalPrice: number;
}
export interface PriceResponse { total: number; limit: number; offset: number; records: MandiRecord[]; source: string; fetchedAt: string }

export interface Variant { variety: string; grade: string }
export interface OptionsResponse { districts: string[]; commodities: string[]; markets: string[]; variants: Variant[]; truncated: boolean; scanned: number }

export type PriceType = 'modal' | 'min' | 'max';
export type Trend = 'RISING' | 'FALLING' | 'STABLE' | 'INSUFFICIENT_DATA';
export interface HistoryRecord { date: string; market: string; price: number }
export interface MarketInfo { market: string; records: number }
export interface MarketStats {
  market: string; dataPoints: number; current: number; currentDate: string; high: number; low: number;
  average: number; changePercent: number | null; trend: Trend;
}
export interface Coverage { firstRecordDate: string | null; lastRecordDate: string | null; daysWithData: number; requestedDays: number; sufficient: boolean; message: string }
export interface HistoryResponse {
  state: string; district: string; commodity: string; variety: string; grade: string; priceType: PriceType;
  period: { from: string; to: string }; aggregationNote: string;
  availableMarkets: MarketInfo[]; selectedMarkets: string[]; availableVariants: Variant[];
  records: HistoryRecord[]; stats: MarketStats[]; coverage: Coverage; source: string; disclaimer: string;
}
export interface ComparisonRow { market: string; modalPrice: number; minPrice: number | null; maxPrice: number | null; arrivalDate: string }
export interface ComparisonResponse {
  state: string; district: string; commodity: string; variety: string | null; grade: string | null;
  latestDate: string | null; rows: ComparisonRow[]; availableVariants: Variant[]; source: string;
}
export interface Alert {
  id: number; state: string; district: string; commodity: string; market: string | null;
  direction: 'ABOVE' | 'BELOW'; threshold: number; triggered: boolean;
  lastObservedPrice: number | null; lastObservedMarket: string | null; lastObservedDate: string | null;
}
