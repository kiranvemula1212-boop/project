# Enfos Reporting Portal — Implementation Spec & Build Prompts

A sequenced build document for Claude Code. Each section contains the **design rationale**
(why this approach, what was rejected), a **paste-ready prompt**, and **acceptance criteria**.

Budget: ~6.5 focused hours. Sections are ordered so that stopping early still leaves a
coherent, demoable application.

---

## 0. How to use this document

1. Create the repo, then save this file as `docs/BUILD_SPEC.md` inside it.
2. Open Claude Code at the repo root. Start each session with:
   > Read `docs/BUILD_SPEC.md`. We are implementing Section N. Follow the section's
   > rationale — do not substitute a different approach without telling me why.
3. Work section by section. Review the diff after each. Do not batch three sections into
   one prompt; the review loop is what keeps quality high.
4. After each backend section, run the tests. After each frontend section, look at it.

**Rule for the whole build:** every non-obvious decision gets a one-line comment explaining
*why*, not *what*. Reviewers read comments looking for reasoning.

---

## Global conventions (apply to every section)

**Backend**
- Java 21, Spring Boot 3.3+, Maven.
- `domain` package has **zero Spring imports**. This is enforced by convention and is the
  reason the query engine is unit-testable without a Spring context.
- Constructor injection only. No `@Autowired` on fields.
- Java `record` for all immutable value types (DTOs, query objects, model).
- No Lombok — records cover it, and one less build dependency for a reviewer.
- Package-private by default; `public` only when crossing a package boundary.

**Frontend**
- React 18 + TypeScript (strict), Vite, React Router v6, TanStack Query v5, Tailwind CSS.
- No component library. A hand-built table is faster here than configuring MUI/shadcn, and
  it shows component design skill, which is explicitly on the rubric.
- No `any`. No `as` casts except at the API boundary with a comment.
- Every component file exports exactly one component.

**Both**
- Names describe domain concepts, not mechanics. `ReportRegistry`, not `ReportManager`.

---

## 1. Repository scaffold

### Rationale

Monorepo with two top-level directories plus infrastructure at the root. A single repo is
correct here — the frontend and backend ship together and version together, and splitting
them would force a reviewer to clone twice.

Rejected: Maven multi-module wrapping the frontend via `frontend-maven-plugin`. It couples
the JS build to the Java build, makes frontend iteration slow, and is a common source of
"works on my machine." Docker Compose is the cleaner seam.

### Prompt

```
Create the repository scaffold for a full-stack reporting portal.

Structure:
  backend/          Spring Boot 3.3 + Java 21 + Maven
  frontend/         React 18 + TypeScript + Vite
  docker-compose.yml
  README.md
  docs/BUILD_SPEC.md   (already present)
  .gitignore
  .editorconfig

Backend: generate a Spring Boot project with dependencies spring-boot-starter-web,
spring-boot-starter-validation, spring-boot-starter-test. Group com.enfos, artifact
reporting-portal, base package com.enfos.reporting. Java 21. Include the Maven wrapper.

Frontend: scaffold with Vite react-ts template. Add react-router-dom, @tanstack/react-query,
tailwindcss with postcss and autoprefixer. Configure Tailwind. Set up path alias @/ -> src/.
Enable TypeScript strict mode plus noUncheckedIndexedAccess.

Create empty package directories in the backend matching this layout, each with a
package-info.java stating the package's responsibility in one sentence:
  com.enfos.reporting.api
  com.enfos.reporting.api.dto
  com.enfos.reporting.api.error
  com.enfos.reporting.domain.model
  com.enfos.reporting.domain.query
  com.enfos.reporting.domain.port
  com.enfos.reporting.application
  com.enfos.reporting.infrastructure.inmemory
  com.enfos.reporting.infrastructure.reports
  com.enfos.reporting.infrastructure.config

Do not write any business logic yet. Verify both projects build.
```

### Acceptance

- `cd backend && ./mvnw -q verify` succeeds.
- `cd frontend && npm run build` succeeds.
- `package-info.java` files exist and read like documentation, not filler.

---

## 2. Domain model

### Rationale

Two distinct models, deliberately not conflated:

- **Domain data** — the actual rows (users, departments, projects).
- **Report metadata** — a description of how a report is *presented*: its columns, their
  types, what is sortable and filterable.

Conflating them is the failure mode that forces a frontend change every time a report is
added. Keeping them apart is the entire extensibility story.

A `ReportRow` is deliberately `Map<String, Object>` rather than a typed entity. This is the
one place where dynamic typing is the right call: the table renders *any* report, so rows
must be schema-agnostic at the transport layer. Type safety is recovered by the
`ColumnDefinition` contract, which is validated at startup.

Rejected alternative: generic `ReportRow<T>` with typed entities. It gives compile-time
safety but forces the controller, service, and serializer to know every report type,
which defeats the purpose. Document this tradeoff — it is the sharpest one in the backend.

`ColumnType` drives **rendering**. `FilterType` drives **the filter control**. They are
separate fields because a DATE renders as a formatted date but filters as a range;
collapsing them creates an immediate design smell.

### Prompt

```
Implement the domain model in com.enfos.reporting.domain. No Spring imports anywhere in
this package.

domain/model:

  enum ColumnType { ID, TEXT, EMAIL, NUMBER, CURRENCY, DATE, ENUM }
    Drives client-side rendering only.

  enum FilterType { NONE, TEXT, ENUM }
    Drives which filter control the client shows. Deliberately excludes DATE_RANGE and
    NUMBER_RANGE — we do not advertise capabilities we have not built. Add a comment
    saying so.

  enum SortDirection { ASC, DESC }

  record EnumOption(String value, String label)

  record ColumnDefinition(
      String key,
      String label,
      ColumnType type,
      boolean sortable,
      boolean searchable,
      FilterType filterType,
      List<EnumOption> options   // non-empty iff filterType == ENUM
  ) {
      compact constructor: validate key is non-blank; validate options is non-empty
      when filterType == ENUM and empty otherwise. Fail fast at startup, not at request time.
  }

  record ReportDefinition(
      String id,               // url-safe slug, e.g. "users"
      String name,
      String description,
      String category,
      List<ColumnDefinition> columns
  ) {
      compact constructor: validate id matches ^[a-z][a-z0-9-]*$, columns non-empty,
      column keys unique. Provide Optional<ColumnDefinition> column(String key).
  }

  ReportRow: record wrapping Map<String, Object> values. Defensive copy on construction,
  unmodifiable on read. Add a static factory `of(Map<String,Object>)`.

  record Page<T>(List<T> content, int number, int size, long totalElements) {
      derived: int totalPages(), boolean hasNext(), boolean hasPrevious().
      totalPages must be 1 when totalElements is 0 — the UI shows "Page 1 of 1", not
      "Page 1 of 0".
  }

domain/query:

  record FilterCriterion(String columnKey, List<String> values)
      Multiple values mean OR within the column.

  record SortSpec(String columnKey, SortDirection direction)

  record ReportQuery(
      String search,                  // nullable, free text
      List<FilterCriterion> filters,  // AND across entries
      List<SortSpec> sorts,
      int page,                       // 0-based
      int size
  ) {
      Static factory ReportQuery.of(...) that normalises nulls to empty collections.
  }

Write a short unit test for the Page derived methods, including the zero-elements case.
```

### Acceptance

- No `org.springframework` import anywhere under `domain/`.
- Invalid `ColumnDefinition` (ENUM with no options) throws at construction.
- `Page` with 0 elements reports `totalPages() == 1`.

---

## 3. Ports and the report registry

### Rationale

This is the seam that makes the claim "swap in any database" true rather than aspirational.

A report is a self-contained module: its metadata and its data access, together. Spring
collects every `ReportModule` bean into a list; the registry indexes them by id and fails
fast on duplicates. **Adding a fourth report is one new `@Component` class and one seed
file. No controller change, no enum to extend, no frontend deploy.**

Rejected alternative: a `@ReportId("users")` annotation scanned reflectively. More magic,
harder to debug, no compile-time safety, and no real benefit over an interface method.

The registry fails at startup on duplicate ids rather than at request time. Startup
failures are cheap; production 500s are not.

### Prompt

```
Implement the port layer and registry.

com.enfos.reporting.domain.port:

  interface ReportDataSource {
      Page<ReportRow> fetch(ReportDefinition definition, ReportQuery query);
  }
      Javadoc: implementations receive an ALREADY-VALIDATED query. Every column key in
      the query is guaranteed to exist on the definition and to be sortable/filterable
      as claimed. Validation lives in the application layer so that every present and
      future adapter inherits it — an adapter must never re-validate or trust raw input.

  interface ReportModule {
      ReportDefinition definition();
      ReportDataSource dataSource();
  }

com.enfos.reporting.application:

  @Component ReportRegistry
      Constructor takes List<ReportModule>. Builds an unmodifiable LinkedHashMap keyed by
      definition id, preserving injection order. Throws IllegalStateException naming the
      duplicate id if two modules collide.
      Methods: List<ReportDefinition> definitions();
               Optional<ReportModule> find(String reportId);

Write a unit test proving duplicate ids fail construction with a message containing the
offending id.
```

### Acceptance

- `ReportRegistry` construction with two modules sharing an id throws, and the message
  names the id.
- `ReportDataSource` javadoc states the validation guarantee explicitly.

---

## 4. In-memory adapter and query engine

### Rationale

The query engine is the most algorithmically interesting code in the backend and the place
where tests pay off most. It compiles a `ReportQuery` into a predicate chain, a comparator,
and a slice.

Two details that separate this from a naive implementation:

1. **Stable sort tiebreaker.** Every comparator chain ends with an ascending comparison on
   the row's identity column. Without it, rows with equal sort values can reorder between
   page requests, so the user sees a duplicate on page 2 and silently never sees another
   row. This bug is invisible with 12 rows of test data.

2. **Null ordering is explicit.** Nulls sort last in both directions (`NULLS LAST`), which
   matches user expectation — an active project with no end date should not lead the list
   when sorting by end date. Comment this; it is a deliberate choice, not a default.

Filtering runs before sorting, sorting before slicing. Obvious, but doing it in the wrong
order is a real bug class.

### Prompt

```
Implement the in-memory adapter in com.enfos.reporting.infrastructure.inmemory.

RowPredicateFactory (package-private, static methods):
  - searchPredicate(ReportDefinition, String term): matches if ANY column with
    searchable=true contains the term, case-insensitive, using String.valueOf on the value.
    Blank or null term returns an always-true predicate.
  - filterPredicate(FilterCriterion): row matches if the column's value, as a string,
    equals ANY of the criterion values (OR within a column). Case-sensitive for enums.
  - Combine all filter criteria with AND.

RowComparatorFactory (package-private):
  - comparatorFor(ReportDefinition, List<SortSpec>): builds a chained Comparator<ReportRow>.
  - Compare by the column's natural type: Comparable for Number/String/temporal values,
    falling back to String comparison. Nulls always last, in both directions.
  - ALWAYS append an ascending comparison on the definition's identity column (the first
    column with type ID) as the final tiebreaker, even when sorts is empty. Add a comment
    explaining that this guarantees stable pagination.

InMemoryReportDataSource implements ReportDataSource:
  - Constructed with an immutable List<ReportRow>.
  - fetch(): filter -> sort -> slice. Compute totalElements from the filtered list BEFORE
    slicing. Return an empty content list (not an error) when page is past the end.

JsonSeedLoader (@Component):
  - Loads a classpath JSON file into List<ReportRow> using Jackson.
  - Dates stay as ISO-8601 strings; numbers deserialize as Integer/Long/BigDecimal.
  - Throws a clear IllegalStateException naming the file if it is missing or malformed.

Unit tests (this is the highest-value test suite in the project — write these properly):
  1. search matches across searchable columns and ignores non-searchable ones
  2. multi-value filter behaves as OR within a column
  3. two filters on different columns behave as AND
  4. sort ascending and descending on a text column
  5. nulls sort last in BOTH directions
  6. two rows with equal sort keys keep a stable relative order across page boundaries
  7. page past the end returns empty content with the correct totalElements
```

### Acceptance

- All seven tests pass.
- Test 6 genuinely exercises pagination — build a fixture with ties spanning a page edge.
- `totalElements` reflects the filtered count, not the raw count.

---

## 5. Application service and query validation

### Rationale

This is the security boundary, and it is worth being explicit about why it lives here.

Filter keys, sort keys, and sort directions arrive as user-controlled strings that end up in
**SQL identifier position** in any future JDBC adapter — a place where bind parameters are
not usable. So they must be validated against an allowlist. That allowlist is exactly the
report's declared `ColumnDefinition` list.

Putting the validation in the service rather than the adapter means **every current and
future adapter inherits it**. An adapter author cannot forget. This is the single most
defensible architectural point in the backend and belongs in the README.

Page size is clamped, not rejected, on the upper bound? No — **rejected with a 400**.
Silently returning fewer rows than requested makes clients think they have all the data.
Fail loudly.

### Prompt

```
Implement com.enfos.reporting.application.

QueryValidator:
  validate(ReportDefinition, ReportQuery) throws InvalidQueryException with a precise,
  user-facing message for each violation:
   - unknown column key in a filter or sort -> "Unknown column 'x' on report 'users'."
   - sort on a column with sortable=false -> "Column 'x' is not sortable."
   - filter on a column with filterType=NONE -> "Column 'x' is not filterable."
   - ENUM filter value not in the column's declared options -> name the allowed values
   - page < 0 -> "Page must be zero or greater."
   - size < 1 or size > maxPageSize -> "Page size must be between 1 and 200."
  Collect ALL violations and report them together rather than failing on the first.
  Users fixing one param at a time is a bad experience.

ReportingProperties (@ConfigurationProperties("reporting")):
  int defaultPageSize = 25;
  int maxPageSize = 200;

ReportService:
  List<ReportDefinition> listReports()
  ReportDefinition getDefinition(String reportId)   throws ReportNotFoundException
  Page<ReportRow> getData(String reportId, ReportQuery query)
      -> resolve module, validate query, delegate to the module's data source.

Exceptions in com.enfos.reporting.application: ReportNotFoundException,
InvalidQueryException (carries a List<String> of violations).

Unit tests: unknown sort column rejected; non-sortable column rejected; oversized page
rejected; multiple violations reported together; valid query passes through untouched.
```

### Acceptance

- A query with two problems returns both messages.
- `ReportService` never passes an unvalidated query to a data source.

---

## 6. REST API layer

### Rationale

Contract decisions, each with the alternative that was rejected:

- **`GET /api/reports/{reportId}` returns rows.** The assessment specifies
  `/api/reports/users`, `/departments`, `/projects`. One path-variable route satisfies all
  three literally *and* demonstrates the extensibility. Rejected `/api/reports/{id}/rows`:
  arguably cleaner REST, but it deviates from a given contract, and quietly "improving" a
  stated spec is a bad signal.
- **Metadata as a sub-resource**, not embedded in the row response. Schema and data have
  different cache lifetimes — the client caches metadata indefinitely and refetches rows
  constantly. Cost is one extra request on first open; the client fires both in parallel.
- **Envelope for collections** (`{ data, page }`) rather than a bare array plus
  `X-Total-Count`. Headers get stripped by proxies and are awkward to consume in JS.
- **RFC 9457 Problem Details** for errors via Spring's built-in `ProblemDetail`. Using the
  standard beats inventing an envelope.
- **Raw values, never preformatted.** The API sends `1250000`, not `"$1,250,000"`.
  Formatting is a presentation concern; the client uses `Intl`. Sending display strings is
  very hard to walk back later.
- **Empty result is 200 with an empty array**, never 204. An empty page is a successful
  answer to a valid question.
- **No `/v1` prefix.** The spec gave exact paths. Note in the README that versioning would
  arrive as a URI prefix or media-type parameter when a breaking change is actually needed.

### Prompt

```
Implement the API layer.

Endpoints:
  GET /api/reports
      200 -> { "data": [ReportSummary...] }
      ReportSummary: { id, name, description, category, columnCount }

  GET /api/reports/{reportId}/metadata
      200 -> { "data": ReportMetadata }
      ReportMetadata: { id, name, description, category, columns: [ColumnDefinition...] }
      ColumnDefinition serializes as
        { key, label, type, sortable, searchable, filterType, options }
      Emit ETag on this response; return 304 on matching If-None-Match. The schema is
      near-static, so this is nearly free and demonstrates cache awareness.

  GET /api/reports/{reportId}
      Query params:
        page    default 0, zero-based
        size    default from config (25), max 200
        sort    repeatable: sort=name,asc&sort=status,desc  (direction optional, default asc)
        search  free text across searchable columns
        filter.<columnKey>=v1,v2   comma = OR within column, AND across columns
      200 -> {
        "data": [ { columnKey: value, ... } ],
        "page": { number, size, totalElements, totalPages, hasNext, hasPrevious }
      }

Implement a ReportQueryArgumentResolver (HandlerMethodArgumentResolver) that builds a
ReportQuery from the request, including collecting all filter.* params. Keep this parsing
OUT of the controller — the controller should read as three short methods. Malformed sort
syntax produces an InvalidQueryException, not a 500.

GlobalExceptionHandler (@RestControllerAdvice) producing application/problem+json:
  ReportNotFoundException -> 404, type ".../problems/report-not-found"
  InvalidQueryException   -> 400, type ".../problems/invalid-query", with an "errors"
                             array of the individual violation messages
  Exception               -> 500, generic detail (never leak stack traces or internals)
All problem responses include "instance" (the request path) and a "traceId" (generate a
UUID, and log it alongside the exception so a support request can be traced).

CORS: allow http://localhost:5173 in a dev profile only, via a config class.

Tests: @WebMvcTest slice tests for
  - list returns 200 and the expected shape
  - unknown report id returns 404 with problem+json content type
  - invalid sort column returns 400 listing the violation
  - metadata returns an ETag, and a matching If-None-Match returns 304
```

### Acceptance

- `curl localhost:8080/api/reports/nope` returns `application/problem+json` with a traceId.
- The controller class is under ~80 lines. If it is longer, parsing leaked into it.

---

## 7. Report modules and seed data

### Rationale

Volumes are deliberately uneven because each size tests a different UI state:

- **Users, ~120 rows** — multi-page pagination, meaningful sorting.
- **Departments, 12 rows** — fewer than one page. Proves the pagination control degrades
  gracefully instead of rendering an awkward "Page 1 of 1" with dead buttons.
- **Projects, ~60 rows** — nullable `endDate` and five status values for rich filtering.

Static JSON, not a random generator: deterministic data means tests assert real values and
screenshots are reproducible. Referential integrity holds — every manager and owner is a
real user, every project department exists. A reviewer who spot-checks will find it
consistent, and inconsistent mock data reads as carelessness.

No empty report is needed to demo the empty state; a filter that matches nothing produces
it naturally, which is more honest.

### Prompt

```
Create three report modules in com.enfos.reporting.infrastructure.reports, each a
@Component implementing ReportModule, plus JSON seeds in
backend/src/main/resources/data/.

users.json — 120 rows. Columns:
  id            ID      sortable, not searchable, filter NONE
  name          TEXT    sortable, searchable, filter TEXT
  email         EMAIL   sortable, searchable, filter NONE
  role          ENUM    sortable, not searchable, filter ENUM
                        options ADMIN/Admin, MANAGER/Manager, ANALYST/Analyst, VIEWER/Viewer
  department    TEXT    sortable, searchable, filter ENUM (options = the 12 dept names)
  status        ENUM    sortable, filter ENUM: ACTIVE/Active, INACTIVE/Inactive, PENDING/Pending
  createdDate   DATE    sortable, filter NONE

departments.json — 12 rows:
  id ID, name TEXT (searchable), manager TEXT (searchable), employeeCount NUMBER,
  location ENUM (filterable, 5-6 real city names), annualBudget CURRENCY

projects.json — 60 rows:
  id ID, name TEXT (searchable), department TEXT (filter ENUM), owner TEXT (searchable),
  status ENUM (PLANNING, ACTIVE, ON_HOLD, COMPLETED, CANCELLED),
  startDate DATE, endDate DATE (null for ~20 in-flight projects)

Data quality requirements — this matters, do not generate lazily:
  - Realistic names, plausible emails derived from names, no "User 1"/"Test Dept".
  - Every department manager is a real user in users.json.
  - Every project owner is a real user; every project department is a real department.
  - employeeCount per department roughly matches the count of users in it.
  - Dates spread over 2022-2026; endDate always after startDate; COMPLETED and CANCELLED
    projects always have an endDate, ACTIVE and PLANNING never do.
  - annualBudget between 250,000 and 4,000,000, varied, not round multiples of 100k.

Each module: definition() returns the ReportDefinition; dataSource() returns an
InMemoryReportDataSource built from the seed loaded at construction time.

Add one integration test (@SpringBootTest) asserting all three reports load, the row counts
are 120/12/60, and every project owner exists in the users report.
```

### Acceptance

- The referential integrity test passes.
- Spot-checking 10 random rows shows no placeholder-looking data.

---

## 8. Frontend foundation and API client

### Rationale

**URL is the source of truth for query state** — page, sort, filters, and search all live in
search params, not component state. This is a small implementation decision with an
outsized payoff: filtered views are shareable, the browser back button behaves correctly,
refresh preserves position, and there is exactly one place state can live so it cannot
desync. It is also the thing that most distinguishes "built a table" from "built a tool."

TanStack Query for server state with `placeholderData: keepPreviousData` so paging does not
blank the table — the old rows stay visible, dimmed, while the next page loads. A full
spinner on every page change feels broken at 100ms and awful at 800ms.

Types are hand-written to mirror the backend DTOs rather than generated. At this scale a
generator is more setup than it saves; note in the README that OpenAPI generation is the
obvious next step.

### Prompt

```
Build the frontend foundation.

src/types/api.ts — mirror the backend contract exactly:
  ColumnType, FilterType, EnumOption, ColumnDefinition, ReportSummary, ReportMetadata,
  PageInfo, ReportDataResponse, ProblemDetail.
  ReportRow = Record<string, string | number | boolean | null>.

src/api/client.ts:
  Typed fetch wrapper. On non-2xx, parse the problem+json body and throw an ApiError
  carrying status, title, detail, and the errors array. On network failure throw an
  ApiError with a distinguishable kind so the UI can say "can't reach the server" rather
  than "something went wrong".
  Base URL from import.meta.env.VITE_API_BASE_URL, defaulting to /api.

src/api/reports.ts:
  fetchReports(): Promise<ReportSummary[]>
  fetchReportMetadata(reportId): Promise<ReportMetadata>
  fetchReportData(reportId, query): Promise<ReportDataResponse>
  Serialize the query to search params exactly as the backend expects, including
  repeated sort params and filter.<key> params.

src/hooks/useReportQuery.ts:
  Reads and writes query state from useSearchParams. Exposes:
    { page, size, sorts, filters, search,
      setPage, toggleSort, setFilter, clearFilter, setSearch, clearAll }
  toggleSort cycles asc -> desc -> none on repeated clicks of the same column.
  Any change to sort, filter, or search resets page to 0 — otherwise the user lands on
  page 5 of a 2-page result and sees an empty table. Comment this.

src/hooks/useReports.ts, useReportMetadata.ts, useReportData.ts:
  TanStack Query hooks. Metadata uses staleTime: Infinity (the schema does not change
  within a session). Data uses placeholderData: keepPreviousData.

Set up the router: / -> ReportsLandingPage, /reports/:reportId -> ReportDetailPage,
* -> NotFoundPage. Wrap the app in QueryClientProvider and a top-level ErrorBoundary.
```

### Acceptance

- Changing a filter updates the URL; pasting that URL in a new tab reproduces the view.
- Browser back steps through query states.

---

## 9. Visual design system

### Rationale

The brief is an internal reporting portal for operations staff who will live in it for
hours. That points away from marketing-site aesthetics and toward an **instrument panel**:
quiet surfaces, high information density, and typography that makes numbers scannable.

The deliberate choices, and why they are not defaults:

- **IBM Plex Sans + IBM Plex Mono.** Plex was commissioned specifically for enterprise
  tooling, which is a real justification rather than a taste claim. Plex Mono carries all
  IDs, numbers, currency, and dates — not for decoration, but because **tabular figures
  align on the decimal**, which is the single biggest readability win in a data table.
  Avoids Inter, which is the default everyone reaches for.
- **Cool neutral palette with one saturated accent.** Status colors must be the only
  saturated things competing for attention in the table body; if the chrome is also
  colorful, the badges stop signaling.
- **Signature element: the active query bar.** A row of removable chips showing exactly
  which filters, search terms, and sorts are applied, mirroring the URL. It makes the
  application's state legible at a glance, which is the core anxiety of any reporting tool
  — "am I looking at all the data, or a filtered slice?" This is the one place to spend
  design effort.

Everything else stays disciplined and quiet.

### Prompt

```
Establish the design system in Tailwind config and src/styles/tokens.css.

Fonts (Google Fonts, preconnect + display=swap):
  IBM Plex Sans 400/500/600 — all UI text
  IBM Plex Mono 400/500     — IDs, numbers, currency, dates
Enable font-variant-numeric: tabular-nums on all mono text.

Palette (CSS custom properties, referenced by Tailwind theme extension):
  --canvas      #F6F7F9    page background
  --surface     #FFFFFF    cards, table
  --border      #E3E6EB    hairlines
  --border-str  #C9CFD8    hover/focus
  --ink         #14161A    primary text
  --ink-muted   #5A6270    secondary text
  --ink-faint   #8A919E    hints, placeholders
  --accent      #3B4FD8    interactive, focus ring
  --accent-soft #EEF0FD    accent backgrounds

Status colors — text on tinted background, from the same family:
  positive  bg #E6F4EC  text #135E38   (ACTIVE, COMPLETED)
  warning   bg #FCF0DC  text #8A5A0B   (PENDING, ON_HOLD)
  danger    bg #FBE9E7  text #9C2A20   (CANCELLED, INACTIVE)
  neutral   bg #EEF0F3  text #4A515C   (PLANNING, VIEWER)
  info      bg #E7F0FA  text #17507F   (ADMIN, MANAGER)

Scale: border-radius 6px for controls, 10px for cards. Spacing on a 4px grid.
Table row height 44px comfortable. Type: 13px table body, 12px labels, 14px body,
20px page titles, 15px card titles.

Quality floor, non-negotiable:
  - Visible focus ring (2px accent, 2px offset) on every interactive element.
  - Respect prefers-reduced-motion: no transitions when set.
  - All transitions 150ms ease-out, and only on color/background/border.
  - Minimum touch target 40px on mobile.

Build these primitives in src/components/ui/: Button (primary/secondary/ghost, sizes
sm/md), Input (with optional leading icon and clear button), Select, Badge (variant from
the status colors), Card, Skeleton, IconButton. Each one file, typed props, forwardRef
where a ref makes sense.
```

### Acceptance

- Tabbing through the app shows a clear focus ring at every stop.
- Numbers in a column visually align.

---

## 10. Landing page

### Rationale

The brief asks that users "understand what reports exist, find a specific one easily, and
get into a report." Three requirements, three design responses:

- **Understand what exists** — group cards by category with a section heading. Grouping
  scales to 30 reports where a flat grid does not, and it costs nothing at three.
- **Find one easily** — instant client-side filter over name and description. Client-side
  is correct here: the full report list is small and already loaded, so a server round trip
  per keystroke would be slower and worse. Note this asymmetry with the table's server-side
  search in the README — the reasoning for choosing differently in the two places is
  exactly what a reviewer wants to see.
- **Get in** — the whole card is the click target, not a "View" link, and it is a real
  anchor so middle-click and cmd-click open a new tab.

Cards over a list: reports are heterogeneous objects with a description, and a description
needs room to breathe. A dense list would suit 50 reports; at 3-10, cards read better.

### Prompt

```
Build the landing page.

Header: product name "Reporting", a one-line subtitle, and the search field.

Search: instant client-side filter over report name and description, case-insensitive.
Debounce is unnecessary — the data is in memory. Show a clear (x) button when non-empty.
Autofocus on desktop only (not mobile — it forces the keyboard up).

Report cards, grouped by category with a small uppercase section label:
  - name (15px, medium)
  - description (13px, muted, 2 lines max with ellipsis)
  - a footer row with column count, in mono
  - hover: border darkens to --border-str, subtle lift via border color only, no shadow
  - the entire card is an <a> to /reports/{id}
Grid: 1 column under 640px, 2 under 1024px, 3 above.

States:
  loading  -> 6 skeleton cards matching the real card dimensions, so nothing shifts on load
  error    -> centered panel: what failed, what to do, and a "Try again" button that
              refetches. Never an apology, never "Oops". Example copy:
              "Can't load reports. The server isn't responding. Try again."
  empty    -> only reachable via search: "No reports match 'xyz'." plus a "Clear search"
              button. An empty screen is an invitation to act, not a dead end.

Accessibility: search input has a visible label or aria-label; results count is announced
via an aria-live="polite" region.
```

### Acceptance

- Skeletons occupy the same space as loaded cards (no layout shift).
- Cmd-click on a card opens a new tab.

---

## 11. The DataTable and its registries

### Rationale

This is the architectural centerpiece of the frontend and the thing that pays off the
metadata-driven backend. **One table component renders any report, including reports that
did not exist when the frontend was built.**

Two registries make that work:

- **Cell renderer registry**, keyed by `ColumnType`. A `Record<ColumnType, CellRenderer>`
  with a text fallback for unknown types. Open/closed: adding a `PERCENTAGE` type means
  adding one entry, touching nothing else.
- **Filter control registry**, keyed by `FilterType`. Same shape, same reasoning.

Rejected alternative: a `switch` in the cell component. Functionally equivalent today, but
it puts every future type in one growing function and invites unrelated logic to accumulate
there. The registry makes each renderer independently testable and independently ownable.

Unknown types must render as text rather than crash. A frontend that breaks when the
backend adds a column is not actually decoupled.

**Mobile: the table becomes stacked cards below 768px**, not a horizontally scrolling table.
Horizontal scroll on a 7-column table is a genuinely bad mobile experience — users cannot
tell what they are missing. Stacked label/value pairs are readable. This is the responsive
decision the rubric is looking for.

### Prompt

```
Build the table system in src/components/table/.

cells/registry.ts:
  type CellRenderer = (value: unknown, column: ColumnDefinition) => ReactNode
  const CELL_RENDERERS: Record<ColumnType, CellRenderer>
  getCellRenderer(type): falls back to TextCell for unknown types — never throw.

Renderers:
  IdCell        mono, muted, right-aligned
  TextCell      plain, truncate with title attribute on overflow
  EmailCell     mailto link, accent color on hover only
  NumberCell    mono, tabular-nums, right-aligned, Intl.NumberFormat
  CurrencyCell  mono, right-aligned, Intl.NumberFormat currency USD, no decimals
  DateCell      mono, Intl.DateTimeFormat medium date; renders an em-dash in --ink-faint
                for null. Never the string "null", never a blank cell — an em-dash
                communicates "intentionally absent".
  EnumCell      Badge, variant resolved by a value->variant map defined on the FRONTEND.
                The API sends values and labels; the client decides what green means.
                Unknown enum values fall back to the neutral variant.

DataTable.tsx:
  Props: { columns, rows, sorts, onToggleSort, isFetching }
  - Header cells: sortable ones are buttons with aria-sort set to
    ascending/descending/none, showing a chevron and, when multiple sorts are active,
    the sort's ordinal position.
  - Column alignment comes from the column type: NUMBER, CURRENCY, and ID right-align.
  - Sticky header on vertical scroll.
  - Zebra striping off; use hairline row borders instead. Stripes fight with status badges.
  - When isFetching is true, apply opacity-60 and pointer-events-none to the tbody so the
    previous page stays readable while the next loads.

DataTableMobile.tsx:
  Below 768px, render each row as a card: a strong primary field (the first TEXT column)
  as the card title, then label/value pairs for the rest. Reuse the same cell renderers —
  the renderers must not know which layout they are in.

TableSkeleton.tsx: header row plus 8 shimmer rows at the real row height.
```

### Acceptance

- Adding a fake column with `type: "UNKNOWN"` to a metadata response renders as text and
  does not crash.
- Resizing across 768px switches layouts with no data loss.

---

## 12. Report detail page and query controls

### Rationale

The **active query bar** is the signature element. It renders one removable chip per active
filter value, search term, and sort, plus a "Clear all". It answers the question that makes
people distrust reporting tools — "is this everything, or a slice?" — without making them
read the URL.

Loading states are differentiated deliberately:

- **First load** — skeleton table. Nothing to preserve.
- **Refetch after a query change** — keep previous rows, dim them, show a thin progress bar.
  Blanking a table the user is reading is disorienting.

Empty states are differentiated too, because they need different actions:

- **No rows at all** (impossible with our seeds, but handle it) — "This report has no data."
- **No rows after filtering** — "No rows match your filters," with a "Clear filters" button.
  The action must be adjacent to the problem.

Error states distinguish network failure from a 4xx: "can't reach the server" and
"that filter isn't valid" need different responses from the user.

### Prompt

```
Build ReportDetailPage.

Layout:
  Breadcrumb: "Reports / {name}" — the first segment is a link back to /.
  Title row: report name, description beneath in muted text.
  Toolbar: search input (server-side, debounced 300ms — comment that this differs from the
           landing page's instant filter because it hits the network), one filter control
           per column with filterType != NONE, and a row-count readout
           ("124 rows" / "18 of 124 rows" when filtered).
  Active query bar: chips for each active filter value, the search term, and each sort.
           Each chip is removable (x). A "Clear all" appears when anything is active.
           The bar collapses entirely when nothing is active — no empty container.
  Table.
  Pagination footer: "Page 3 of 5", previous/next buttons, and a page-size select
           (25 / 50 / 100). Hide the whole footer when totalPages is 1 — do not render
           dead controls.

Filter controls from a registry keyed by FilterType, mirroring the cell registry:
  ENUM -> multi-select dropdown with checkboxes, showing the count of selected values on
          the trigger. Closes on outside click and on Escape. Keyboard navigable.
  TEXT -> text input, debounced 300ms
  NONE -> renders nothing

States:
  first load     -> TableSkeleton with the real column headers already visible (metadata
                    resolves before data), so the user sees the shape immediately
  refetching     -> previous rows dimmed, 2px indeterminate progress bar under the toolbar
  empty (no data)-> "This report has no data yet."
  empty (filtered)-> "No rows match your filters." + "Clear filters" button
  error 4xx      -> the problem detail's title and detail, plus "Clear filters"
  error network  -> "Can't reach the server." + "Try again"
  unknown report -> 404 panel with a link back to all reports

Fire the metadata and data queries in parallel — do not chain them.

Accessibility: the table has a <caption> (visually hidden) naming the report; the row-count
readout is aria-live="polite"; Escape closes any open filter dropdown; focus returns to the
trigger on close.
```

### Acceptance

- Removing a chip updates the table and the URL.
- Changing a filter while on page 3 resets to page 1.
- Column headers are visible during the first skeleton load.

---

## 13. Docker and single-command startup

### Rationale

The spec is explicit that a reviewer must bring up the full stack from a clean checkout
without hunting for missing pieces. A surprising number of submissions fail here, and it is
the cheapest possible place to lose points.

Multi-stage builds so the reviewer needs neither a JDK nor Node — only Docker. Nginx serves
the built frontend and proxies `/api` to the backend, which means **no CORS in production
and no environment-specific API URL**. That is how it would actually be deployed.

Healthcheck on the backend with `depends_on: condition: service_healthy`, so the frontend
never comes up pointing at a backend that is not ready. Without it there is a race that
shows a broken UI on first load roughly one time in three.

### Prompt

```
Containerize the application.

backend/Dockerfile — multi-stage:
  Stage 1: maven:3.9-eclipse-temurin-21. Copy pom.xml first and run dependency:go-offline
           so the dependency layer caches. Then copy src and package.
  Stage 2: eclipse-temurin:21-jre-alpine. Copy the jar. Run as a non-root user.
           EXPOSE 8080. Add a HEALTHCHECK hitting /actuator/health.
  Add spring-boot-starter-actuator and expose only the health endpoint.

frontend/Dockerfile — multi-stage:
  Stage 1: node:20-alpine. npm ci, then npm run build.
  Stage 2: nginx:alpine. Copy dist to /usr/share/nginx/html plus a custom nginx.conf that
           (a) proxies /api to http://backend:8080, and
           (b) falls back to index.html for client-side routes, so a hard refresh on
               /reports/users works instead of 404ing.

docker-compose.yml:
  backend  — build ./backend, port 8080, healthcheck
  frontend — build ./frontend, port 3000:80, depends_on backend with
             condition: service_healthy
  A named network. No volumes needed.

Also add a Makefile with: make up, make down, make logs, make test, make dev.

Then verify properly: clone the repo into a fresh temp directory, run docker compose up
--build there, and confirm the app works end to end at localhost:3000. Do not verify in
the working directory — stale node_modules and target/ hide broken builds.
```

### Acceptance

- `git clone` to `/tmp`, `docker compose up --build`, working app at `localhost:3000`.
- A hard refresh on `/reports/users` loads the page rather than 404ing.

---

## 14. README

### Rationale

This is not documentation overhead — it is the graded artifact where reasoning is visible.
The code shows *what* was built; the README is the only place to show *why*, and the brief
says explicitly that reasoning is what they care about.

The "deliberately out of scope" section is the most senior part of the document. Naming
what you chose not to build, and why, demonstrates judgment more reliably than any feature
does.

### Prompt

```
Write the README with these sections, in this order:

1. What this is — two sentences, plus one screenshot of the landing page and one of a
   report view.

2. Run it — prerequisites (Docker only), the single command, the URL. Then a "Run without
   Docker" subsection for anyone who wants to iterate.

3. Architecture — a short diagram or tree, then a paragraph on the port seam: a report is a
   self-contained module implementing ReportModule; ReportRegistry collects them at
   startup; adding a report is one @Component class and one seed file, with no controller,
   frontend, or configuration change.

4. Adding a new report — an actual worked example with the code, ~15 lines. Prove the
   claim rather than asserting it.

5. Swapping the data source — explain that ReportDataSource is the port, that
   InMemoryReportDataSource is one adapter, and exactly what a JDBC adapter would
   implement. State clearly that query validation lives in the application layer
   specifically so that any future adapter inherits it, since filter and sort keys land in
   SQL identifier position where bind parameters cannot be used.

6. API reference — the three endpoints, the query parameter grammar, filter semantics
   (comma = OR within a column, AND across columns), the response envelope, and the
   problem+json error shape. Include two curl examples.

7. Design decisions and tradeoffs — the honest section. Cover:
   - Metadata-driven columns vs three bespoke tables, and the cost (dynamic ReportRow
     typing at the transport layer)
   - Offset pagination vs cursor, and when you would switch
   - The stable sort tiebreaker and the pagination bug it prevents
   - Metadata as a separate cacheable endpoint vs embedding it
   - Client-side search on the landing page vs server-side search in the table, and why
     the same problem got two different answers
   - URL as the single source of truth for query state
   - Raw values over the wire rather than preformatted display strings

8. Deliberately out of scope — auth and RBAC, CSV export, column visibility persistence,
   virtualized rows, caching tier, a live JDBC adapter. One line each on why it was not
   worth building for this scope, and what it would take to add.

9. Testing — what is covered and why those areas specifically.

Tone: plain, direct, no marketing language. Sentence case headings.
```

### Acceptance

- A reader who has never seen the code can add a fourth report from the README alone.
- Section 7 reads like an engineer thinking, not a feature list.

---

## Cut list, if time runs short

Cut in this order. Each cut keeps the architecture intact and gets documented in the README
as a conscious decision rather than an omission.

1. ETag / 304 on the metadata endpoint
2. The mobile card layout (fall back to a horizontally scrolling table, and say so)
3. Multi-column sort (keep single-column)
4. The page-size selector (fix at 25)
5. TEXT filters (keep ENUM only — it still proves the registry)

**Never cut:** the port seam, the metadata-driven table, the query engine tests, Docker
Compose, or the README tradeoffs section. Those are the graded surface.

---

## Final verification checklist

- [ ] Fresh clone into a new directory, `docker compose up --build`, works end to end
- [ ] All three reports load, sort, filter, search, and paginate
- [ ] Empty state reachable via a filter that matches nothing
- [ ] Error state reachable by stopping the backend container
- [ ] Hard refresh on `/reports/projects` loads correctly
- [ ] Filtered URL pasted into a new tab reproduces the exact view
- [ ] Keyboard-only pass: tab to a report, open it, sort a column, open and apply a filter
- [ ] 375px viewport is usable
- [ ] `./mvnw verify` passes with no skipped tests
- [ ] No `console.log`, no commented-out code, no TODOs left in the diff
- [ ] Screenshots or a 2-3 minute walkthrough recorded
