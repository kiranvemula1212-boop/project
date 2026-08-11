import { useEffect, useRef } from "react";
import { Link, useParams } from "react-router-dom";
import { ApiError } from "@/api/client";
import { ActiveQueryBar } from "@/components/report-detail/ActiveQueryBar";
import { MessagePanel } from "@/components/report-detail/MessagePanel";
import { PaginationFooter } from "@/components/report-detail/PaginationFooter";
import { ReportToolbar } from "@/components/report-detail/ReportToolbar";
import { DataTable } from "@/components/table/DataTable";
import { DataTableMobile } from "@/components/table/DataTableMobile";
import { TableSkeleton } from "@/components/table/TableSkeleton";
import { useReportData } from "@/hooks/useReportData";
import { useReportMetadata } from "@/hooks/useReportMetadata";
import { useReportQuery } from "@/hooks/useReportQuery";

const DEFAULT_PAGE_SIZE = 25;

function describeError(error: unknown): { title: string; isNetwork: boolean } {
  if (error instanceof ApiError) {
    if (error.kind === "network") {
      return { title: "Can't reach the server.", isNetwork: true };
    }
    return { title: error.detail ?? error.title ?? "That query isn't valid.", isNetwork: false };
  }
  return { title: "Something went wrong.", isNetwork: false };
}

// Fires metadata and data in parallel (two independent hooks, neither gated on the
// other) rather than chaining them — the schema and the rows have nothing to wait on
// each other for.
export function ReportDetailPage() {
  const { reportId = "" } = useParams<{ reportId: string }>();
  const metadataQuery = useReportMetadata(reportId);
  const query = useReportQuery(DEFAULT_PAGE_SIZE);
  const dataQuery = useReportData(reportId, {
    search: query.search,
    filters: query.filters,
    sorts: query.sorts,
    page: query.page,
    size: query.size,
  });

  const isFiltered = query.search.trim() !== "" || Object.values(query.filters).some((values) => values.length > 0);

  // Remembers the report's unfiltered total the first time it's seen, so a later
  // filtered view can show "18 of 124 rows" instead of just "18 rows". If the user
  // arrives on an already-filtered URL, there's nothing to remember yet, and the count
  // degrades gracefully to just the filtered number.
  const baseTotalRef = useRef<number | undefined>(undefined);
  useEffect(() => {
    if (dataQuery.data && !isFiltered) {
      baseTotalRef.current = dataQuery.data.page.totalElements;
    }
  }, [dataQuery.data, isFiltered]);

  if (metadataQuery.isError) {
    const notFound = metadataQuery.error instanceof ApiError && metadataQuery.error.status === 404;
    return (
      <div className="mx-auto max-w-5xl px-6 py-10">
        {notFound ? (
          <MessagePanel
            title="Report not found."
            description={`There's no report at "${reportId}".`}
            linkAction={{ label: "Back to all reports", to: "/" }}
          />
        ) : (
          <MessagePanel
            title={describeError(metadataQuery.error).title}
            action={{ label: "Try again", onClick: () => void metadataQuery.refetch() }}
          />
        )}
      </div>
    );
  }

  if (metadataQuery.isPending) {
    return (
      <div className="mx-auto max-w-5xl px-6 py-10">
        <div className="h-6 w-40 animate-pulse rounded-control bg-border" />
        <div className="mt-8 h-64 animate-pulse rounded-card border border-border bg-surface" />
      </div>
    );
  }

  const metadata = metadataQuery.data;
  const rowCountLabel = dataQuery.data
    ? isFiltered && baseTotalRef.current !== undefined
      ? `${dataQuery.data.page.totalElements} of ${baseTotalRef.current} rows`
      : `${dataQuery.data.page.totalElements} rows`
    : "";
  const dataError = dataQuery.isError ? describeError(dataQuery.error) : null;

  return (
    <div className="mx-auto max-w-5xl px-6 py-10">
      <nav aria-label="Breadcrumb" className="text-label text-ink-faint">
        <Link to="/" className="hover:text-ink-muted">
          Reports
        </Link>
        <span className="mx-1">/</span>
        <span className="text-ink-muted">{metadata.name}</span>
      </nav>

      <div className="mt-2">
        <h1 className="text-page-title font-semibold text-ink">{metadata.name}</h1>
        <p className="mt-1 text-body text-ink-muted">{metadata.description}</p>
      </div>

      <ReportToolbar
        columns={metadata.columns}
        search={query.search}
        filters={query.filters}
        onSearchChange={query.setSearch}
        onFilterChange={query.setFilter}
        rowCountLabel={rowCountLabel}
      />

      <ActiveQueryBar
        columns={metadata.columns}
        search={query.search}
        filters={query.filters}
        sorts={query.sorts}
        onRemoveFilterValue={(columnKey, value) =>
          query.setFilter(columnKey, (query.filters[columnKey] ?? []).filter((v) => v !== value))
        }
        onClearSearch={() => query.setSearch("")}
        onRemoveSort={query.removeSort}
        onClearAll={query.clearAll}
      />

      {dataQuery.isFetching && (
        <div className="h-0.5 w-full overflow-hidden bg-accent-soft">
          <div className="h-full w-1/3 animate-indeterminate-progress bg-accent" />
        </div>
      )}

      <div className="mt-4">
        {dataQuery.isPending && <TableSkeleton columns={metadata.columns} />}

        {dataError && (
          <MessagePanel
            title={dataError.title}
            action={
              dataError.isNetwork
                ? { label: "Try again", onClick: () => void dataQuery.refetch() }
                : { label: "Clear filters", onClick: query.clearAll }
            }
          />
        )}

        {dataQuery.data && dataQuery.data.data.length === 0 && !isFiltered && (
          <MessagePanel title="This report has no data yet." />
        )}

        {dataQuery.data && dataQuery.data.data.length === 0 && isFiltered && (
          <MessagePanel title="No rows match your filters." action={{ label: "Clear filters", onClick: query.clearAll }} />
        )}

        {dataQuery.data && dataQuery.data.data.length > 0 && (
          <>
            <div className="hidden overflow-hidden rounded-card border border-border bg-surface md:block">
              <DataTable
                columns={metadata.columns}
                rows={dataQuery.data.data}
                sorts={query.sorts}
                onToggleSort={query.toggleSort}
                isFetching={dataQuery.isFetching}
                caption={`${metadata.name} report`}
              />
            </div>
            <div className="md:hidden">
              <DataTableMobile columns={metadata.columns} rows={dataQuery.data.data} />
            </div>

            <div className="mt-4">
              <PaginationFooter
                page={dataQuery.data.page.number}
                size={query.size}
                totalPages={dataQuery.data.page.totalPages}
                hasNext={dataQuery.data.page.hasNext}
                hasPrevious={dataQuery.data.page.hasPrevious}
                onPageChange={query.setPage}
                onSizeChange={query.setSize}
              />
            </div>
          </>
        )}
      </div>
    </div>
  );
}
