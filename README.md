# Mandi Market Price & Historical Price Analysis

Standalone web app: current mandi prices, market comparison and historical trends from the official
**data.gov.in** dataset *"Current Daily Price of Various Commodities from Various Markets (Mandi)"*.
History is built by the app itself (a daily sync stores real records in PostgreSQL); nothing is fabricated.

```
React (Vite, TS)  ->  Spring Boot  ->  data.gov.in
                          |
                      PostgreSQL (mandi_price_history, price_alert)
```
The browser never talks to data.gov.in and never sees the API key.

## Folder structure
```
mandi-price-app/
├── .env.example            # copy to .env, add your key (never commit .env)
├── docker-compose.yml      # PostgreSQL 16
├── backend/                # Spring Boot 3.3, Java 17, Maven
│   └── src/main/java/com/mandi/
│       ├── client/         # DataGovMandiClient (ONLY place that knows the upstream format)
│       ├── config/         # properties, RestClient w/ timeouts, cache, CORS
│       ├── controller/     # MandiPriceController, HistoricalPriceController, AlertController
│       ├── service/        # prices, options, history, comparison, ingest(upsert), sync, alerts, statistics
│       ├── repository/ entity/ dto/ scheduler/ exception/
│   └── src/main/resources/ # application.yml, db/migration/V1__init.sql (Flyway)
└── frontend/               # React + Vite + TypeScript + Recharts
    └── src/{api.ts, App.tsx, components/, test/}
```

## Run it
1. Get a key: https://data.gov.in -> sign in -> *My Account* -> API key. (The public sample key returns only ~10 rows; use your own.)
2. Database: `docker compose up -d`
3. Backend (Windows PowerShell: `$env:DATA_GOV_API_KEY="..."`):
   ```
   cd backend
   export DATA_GOV_API_KEY=your-secret-key
   mvn spring-boot:run
   ```
4. Frontend:
   ```
   cd frontend
   npm install
   npm run dev          # http://localhost:5173  (proxies /api -> :8080)
   ```
5. Fill history: set `MANDI_ADMIN_TOKEN=secret` and `curl -X POST -H "X-Admin-Token: secret" "http://localhost:8080/api/mandi/sync?states=Maharashtra"`,
   or set `MANDI_SYNC_ON_STARTUP=true`. The scheduled job (`MANDI_SYNC_CRON`, default 19:30 IST) then runs daily.
   Every search in the UI also stores the real rows it fetched.

Tests: `cd backend && mvn test` · `cd frontend && npm test`

## Endpoints
| Endpoint | Purpose |
|---|---|
| `GET /api/mandi/states` | state list (config; geography only) |
| `GET /api/mandi/options?state&district&commodity` | districts / commodities / markets / varieties discovered from real API rows |
| `GET /api/mandi/prices?state&district&commodity&market&variety&grade&limit&offset` | current prices, paginated (limit 1–100, default 20) |
| `GET /api/mandi/history?state&district&commodity&market&markets&variety&grade&from&to&priceType` | stored history, stats, trend, coverage |
| `GET /api/mandi/compare?state&district&commodity&variety&grade` | latest reported modal price per market |
| `GET/POST/DELETE /api/alerts` | basic price alerts |
| `POST /api/mandi/sync` | manual sync (disabled unless `MANDI_ADMIN_TOKEN` set) |

## How the what-if scenarios are handled
| # | Scenario | Handling |
|---|---|---|
| 1 | Commodity unavailable | Dropdowns come from API rows; empty result -> "No mandi price records found…" (never a default price) |
| 2 | 10,000 records | `limit/offset` pagination; history is filtered/limited in SQL (top 5 markets by default, max 8) |
| 3, 10 | Different market prices | Wording: "X currently has the highest reported modal price among the markets listed" + cost disclaimer |
| 4 | Name mismatch ("Tur" vs "Red gram/Arhar/Tur(whole)") | Typeahead filters the real API names; search only accepts an official name. No dictionary |
| 5 | API down | 2 attempts, timeouts, generic 503 message, no stack traces to the client |
| 6 | Key exposure | Key only in backend env; never in responses; URLs/errors sanitised in logs |
| 7 | Response changes | All mapping isolated in `DataGovMandiClient` (case-insensitive, ignores unknown fields) |
| 8 | 100 concurrent users | Caffeine cache keyed by normalized query (10 min; option lists 6 h). No Redis |
| 9 | Invalid prices | null/0/negative/non-numeric modal price or bad date -> row dropped (counted as invalid); min>max -> min/max ignored |
| 11, 13 | Missing dates / closed market | No interpolation; chart has one slot per calendar day and leaves gaps; UI says gaps ≠ ₹0 |
| 12 | Multiple varieties | Never merged. User picks variety/grade; default = most frequently reported, stated in the UI |
| 14 | Not enough history | `coverage.sufficient=false` + message ("Not enough history yet for a 180-day view…") |
| 15 | Source corrections | Upsert on natural key `state+district+market+commodity+variety+grade+arrival_date`: changed -> UPDATE, same -> skipped. DB unique constraint backs it |

## Design notes
* **Stats** are per market (never averaged across markets). Trend = average of the recent half vs the earlier half of the reported points; threshold `MANDI_TREND_THRESHOLD` (default ±3%); needs ≥4 points. Shown as a *historical indicator*, never a forecast.
* **Indexes**: `(state,district,commodity,arrival_date)`, `(state,district,commodity,market,arrival_date)`, `(arrival_date)`. For growth, add date partitioning or archive rows older than N years when needed.
* **Alerts (v1)**: stored and evaluated after each sync; no accounts, no email/SMS. Because there is no authentication, alerts are global to the instance. Add auth before multi-user use.
* **Options discovery** scans up to `mandi.discovery.max-records` (5000) rows per filter set and caches it; a "partial scan" hint shows if the cap was hit.
* The state list in `application.yml` follows the dataset's spellings (e.g. "Chattisgarh", "Uttrakhand", "NCT of Delhi"). If the dataset uses another spelling, edit `mandi.states`.
* Lombok is used on entities (install the IDE plugin).
