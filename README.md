# Enfos reporting portal

An internal reporting portal: pick a report from a landing page, then sort, filter,
search, and page through it. Adding a new report is one backend module and a seed file —
no frontend change required.

![Landing page](docs/screenshots/landing.png)
![Report view](docs/screenshots/report-detail.png)

## Run it

Prerequisites: Docker only.

```
docker compose up --build
```

Open http://localhost:3000.

### Run without Docker

Prerequisites: Java 21, Node 20+ (the Maven wrapper is included, so no local Maven
install is needed).

```
# terminal 1
cd backend
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run

# terminal 2
cd frontend
npm install
npm run dev
```

Open http://localhost:5173. The `dev` profile enables CORS for the Vite dev server
(5173) to call the backend (8080) directly; in the Docker build there is no CORS
configuration at all, because Nginx proxies `/api` on the same origin as the built
frontend.

## Architecture

```
backend/src/main/java/com/enfos/reporting/
  api/                    controllers, DTOs, problem+json error handling
  application/            ReportService, ReportRegistry, QueryValidator
  domain/
    model/                ColumnDefinition, ReportDefinition, ReportRow, Page — zero Spring imports
    query/                ReportQuery, FilterCriterion, SortSpec
    port/                 ReportDataSource, ReportModule — the seam
  infrastructure/
    inmemory/              the query engine: predicates, comparators, JSON seed loading
    reports/               the three concrete report modules + department options
    config/                CORS, the custom query argument resolver

frontend/src/
  api/                    typed fetch client
  hooks/                  useReportQuery (URL state), TanStack Query hooks
  components/
    ui/                   Button, Input, Select, Badge, Card, Skeleton, IconButton
    table/                DataTable, DataTableMobile, TableSkeleton, cell registry
    filters/               filter control registry
    report-detail/         toolbar, active query bar, pagination footer
  pages/                  ReportsLandingPage, ReportDetailPage, NotFoundPage
```

A report is a self-contained module: its metadata (`ReportDefinition`, describing
columns, types, and what's sortable/filterable) and its data access
(`ReportDataSource`), bundled together as a `ReportModule`. Spring collects every
`ReportModule` bean into a `ReportRegistry` at startup, which indexes them by id and
fails fast — an `IllegalStateException` naming the offending id — if two modules
declare the same one.

The payoff: **adding a report is one new `@Component` class and one seed file.** No
controller change, no enum to extend, no frontend deploy — the frontend renders any
report's columns generically, driven entirely by the metadata the backend sends.

## Adding a new report

This is a complete, working report module — not a sketch of one:

```java
@Component
class VendorsReportModule implements ReportModule {
    private final ReportDefinition definition = new ReportDefinition(
        "vendors", "Vendors", "Approved vendors and their contract status.", "Operations",
        List.of(
            new ColumnDefinition("id", "ID", ColumnType.ID, true, false, FilterType.NONE, List.of()),
            new ColumnDefinition("name", "Name", ColumnType.TEXT, true, true, FilterType.TEXT, List.of()),
            new ColumnDefinition("status", "Status", ColumnType.ENUM, true, false, FilterType.ENUM,
                List.of(new EnumOption("ACTIVE", "Active"), new EnumOption("EXPIRED", "Expired")))
        ));
    private final ReportDataSource dataSource;

    VendorsReportModule(JsonSeedLoader seedLoader) {
        this.dataSource = new InMemoryReportDataSource(seedLoader.load("data/vendors.json"));
    }

    @Override public ReportDefinition definition() { return definition; }
    @Override public ReportDataSource dataSource() { return dataSource; }
}
```

Drop `vendors.json` (an array of row objects matching the column keys above) into
`backend/src/main/resources/data/`, and the report appears on the landing page, sorts,
filters, and paginates — with zero other changes.

## Swapping the data source

`ReportDataSource` is the port:

```java
public interface ReportDataSource {
    Page<ReportRow> fetch(ReportDefinition definition, ReportQuery query);
}
```

`InMemoryReportDataSource` is the only adapter today. A JDBC adapter would implement the
same interface — translating `ReportQuery`'s filters and sorts into a `WHERE`/`ORDER BY`
clause and running it against a table — and the application and API layers would not
change at all.

Query validation deliberately lives in the **application** layer
(`QueryValidator`), not in any adapter. Filter keys, sort keys, and sort directions
arrive as user-controlled strings that would land in SQL identifier position in a JDBC
adapter — a place bind parameters cannot protect. Validating centrally, against exactly
the report's declared `ColumnDefinition`s, means every present and future adapter
inherits that guarantee automatically. An adapter author cannot forget to check what a
request is asking to touch, because it never sees an unvalidated query.

## API reference

Base path: `/api/reports`.

- `GET /api/reports` — list every report's summary (id, name, description, category, column count).
- `GET /api/reports/{reportId}/metadata` — that report's full column definitions. Emits
  an `ETag`; a matching `If-None-Match` gets a `304`.
- `GET /api/reports/{reportId}` — row data. Query params:
  - `page` (default `0`), `size` (default `25`, max `200`)
  - `sort=columnKey,direction` — repeatable, direction optional (defaults to `asc`), e.g. `sort=name,asc&sort=status,desc`
  - `search` — free text across the report's searchable columns
  - `filter.<columnKey>=v1,v2` — comma-separated values are OR'd within a column; separate `filter.*` params are AND'd across columns

Collection responses are an envelope: `{ "data": [...], "page": { number, size,
totalElements, totalPages, hasNext, hasPrevious } }`. Errors are RFC 9457
`application/problem+json`, always carrying a `traceId` that's also written to the
server log, so a support request can be traced back to the exact log line.

```
curl 'localhost:8080/api/reports/users?sort=name,asc&filter.status=ACTIVE&size=5'

curl -i 'localhost:8080/api/reports/nope'
# HTTP/1.1 404
# Content-Type: application/problem+json
# {"type":"/problems/report-not-found","title":"Report not found","status":404,
#  "detail":"No report found with id 'nope'.","instance":"/api/reports/nope",
#  "traceId":"..."}
```

## Design decisions and tradeoffs

**Metadata-driven columns, not three bespoke tables.** The cost is that `ReportRow` is a
`Map<String, Object>`, not a typed entity — the transport layer is deliberately
schema-agnostic because the table has to render *any* report, including ones that don't
exist yet. Type safety is recovered by the `ColumnDefinition` contract, which is
validated at construction time, not at request time.

**Offset pagination, not cursor.** Offset (`page`/`size`) is simpler to reason about, to
link to, and to build a page-size selector against, and it's the right choice for
bounded, moderate-sized reports like these. It would stop being the right choice for a
report with unbounded growth or where rows are inserted/deleted while a user pages
through — cursor pagination avoids the "page drifts as data changes underneath you"
problem, at the cost of losing "jump to page 7."

**The stable sort tiebreaker.** Every comparator chain ends with an ascending compare on
the identity column, even when no sort was requested. Without it, rows with equal sort
keys can reorder between page requests — invisible with a handful of test rows, and a
real bug with 120.

**Metadata as a separate, cacheable endpoint** rather than embedded in every row
response. Schema and data have different lifetimes: the client caches metadata
indefinitely (`staleTime: Infinity`) and refetches rows on every query change. The cost
is one extra request on first open; the client fires it in parallel with the data
request, not in sequence.

**Client-side search on the landing page, server-side search in the table.** The same
"search" problem gets two different answers, deliberately: the landing page's report
list is small and already loaded, so filtering it client-side is strictly faster than a
round trip. A report's *rows* are not preloaded — only the current page is — so search
has to hit the server.

**URL as the single source of truth for query state.** Page, sort, filters, and search
all live in the URL's search params, not component state. A filtered view is shareable,
the back button works, and a refresh preserves position — because there is exactly one
place this state can live, so it cannot desync from what's on screen.

**Raw values over the wire, never preformatted display strings.** The API sends
`1250000`, not `"$1,250,000"`. Formatting is a presentation concern handled by `Intl` on
the client; sending display strings would be very hard to walk back once a consumer
depends on them.

## Deliberately out of scope

- **Auth and RBAC** — every report is visible to every caller. A real deployment would
  add a Spring Security filter chain and per-report authorization checks in
  `ReportService`; nothing in the port design blocks that.
- **CSV export** — would be a new controller endpoint reusing the same validated
  `ReportQuery` and streaming `ReportRow`s through a CSV writer instead of JSON.
- **Column visibility persistence** — no per-user preference storage exists yet; would
  need a user identity to key it on, which auth doesn't exist for either.
- **Virtualized rows** — at 25–200 rows per page this isn't a performance problem yet.
  Worth adding if a report's max page size grows substantially.
- **A caching tier** — the in-memory adapter is already as fast as caching would make
  it. A real JDBC adapter behind a slow database is where a cache would earn its keep.
- **A live JDBC adapter** — the port is designed for it (see "Swapping the data
  source"), but building and testing one was out of scope for this exercise.

## Testing

The query engine (`RowPredicateFactory`, `RowComparatorFactory`,
`InMemoryReportDataSource`) is the highest-value suite in the project and is tested most
thoroughly: search-vs-non-searchable columns, OR-within/AND-across filters, ascending and
descending sort, nulls-last in both directions, and — the one that actually catches
bugs — a fixture built so a tie in sort key spans a page boundary, proving the stable
tiebreaker prevents rows from being skipped or duplicated across pages.

Above that: domain model validation (invalid `ColumnDefinition`/`ReportDefinition`
constructions fail fast), `QueryValidator` (every violation type, and that multiple
violations are collected together rather than failing on the first), `ReportRegistry`
(duplicate ids), and `@WebMvcTest` slices for the controller's HTTP contract (shape,
404, 400, ETag/304). An `@SpringBootTest` integration test asserts all three seeded
reports load with the right row counts and that every cross-reference (project owner,
department manager) resolves to a real user — the referential-integrity guarantee the
seed data promises.

The frontend has no automated test suite; TypeScript strict mode plus manual
browser verification (every interaction in this README's screenshots, plus the mobile
layout, keyboard navigation, and the Docker build) stood in for it here. Given more time,
component tests for the cell/filter registries and the query-state hook would be the
first addition.
