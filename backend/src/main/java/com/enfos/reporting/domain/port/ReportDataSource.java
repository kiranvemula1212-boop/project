package com.enfos.reporting.domain.port;

import com.enfos.reporting.domain.model.Page;
import com.enfos.reporting.domain.model.ReportDefinition;
import com.enfos.reporting.domain.model.ReportRow;
import com.enfos.reporting.domain.query.ReportQuery;

/**
 * The seam that lets any storage technology back a report. An in-memory adapter is the
 * only implementation today; a JDBC adapter would implement the same interface with no
 * change to the application or API layers.
 */
public interface ReportDataSource {

    /**
     * Fetches a page of rows matching {@code query}.
     *
     * <p>Implementations receive an ALREADY-VALIDATED query: every column key referenced
     * by a filter or sort is guaranteed to exist on {@code definition} and to be
     * sortable/filterable as claimed, and paging bounds are guaranteed sane. Validation
     * lives in the application layer so that every present and future adapter inherits
     * it — an adapter must never re-validate or trust raw input, and in particular must
     * never treat a column key as anything other than a value already checked against an
     * allowlist (this matters most for a JDBC adapter, where column keys land in SQL
     * identifier position).
     */
    Page<ReportRow> fetch(ReportDefinition definition, ReportQuery query);
}
