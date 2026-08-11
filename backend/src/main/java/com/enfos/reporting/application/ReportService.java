package com.enfos.reporting.application;

import com.enfos.reporting.domain.model.Page;
import com.enfos.reporting.domain.model.ReportDefinition;
import com.enfos.reporting.domain.model.ReportRow;
import com.enfos.reporting.domain.port.ReportModule;
import com.enfos.reporting.domain.query.ReportQuery;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * The only path from the API layer into report data. Every query is validated here
 * before it ever reaches a {@code ReportDataSource} — an adapter never sees an
 * unvalidated query.
 */
@Service
public class ReportService {

    private final ReportRegistry registry;
    private final QueryValidator validator;

    ReportService(ReportRegistry registry, QueryValidator validator) {
        this.registry = registry;
        this.validator = validator;
    }

    public List<ReportDefinition> listReports() {
        return registry.definitions();
    }

    public ReportDefinition getDefinition(String reportId) {
        return resolveModule(reportId).definition();
    }

    public Page<ReportRow> getData(String reportId, ReportQuery query) {
        ReportModule module = resolveModule(reportId);
        validator.validate(module.definition(), query);
        return module.dataSource().fetch(module.definition(), query);
    }

    private ReportModule resolveModule(String reportId) {
        return registry.find(reportId).orElseThrow(() -> new ReportNotFoundException(reportId));
    }
}
