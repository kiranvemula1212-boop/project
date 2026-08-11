package com.enfos.reporting.domain.port;

import com.enfos.reporting.domain.model.ReportDefinition;

/**
 * A report is a self-contained module: its metadata and its data access, together.
 * Adding a report is one new implementation of this interface plus a seed — no controller
 * change, no enum to extend, no frontend deploy.
 */
public interface ReportModule {

    ReportDefinition definition();

    ReportDataSource dataSource();
}
