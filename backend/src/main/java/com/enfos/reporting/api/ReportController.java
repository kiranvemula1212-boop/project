package com.enfos.reporting.api;

import com.enfos.reporting.api.dto.DataEnvelope;
import com.enfos.reporting.api.dto.DataPageEnvelope;
import com.enfos.reporting.api.dto.PageInfo;
import com.enfos.reporting.api.dto.ReportMetadata;
import com.enfos.reporting.api.dto.ReportSummary;
import com.enfos.reporting.application.ReportService;
import com.enfos.reporting.domain.model.Page;
import com.enfos.reporting.domain.model.ReportDefinition;
import com.enfos.reporting.domain.model.ReportRow;
import com.enfos.reporting.domain.query.ReportQuery;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * One path-variable route ({@code /api/reports/{reportId}}) for row data, satisfying the
 * three literal report endpoints the brief specifies while also demonstrating that adding
 * a report needs no controller change. Metadata is a separate sub-resource rather than
 * embedded in the row response — schema and data have different cache lifetimes, so the
 * client caches metadata indefinitely and refetches rows constantly.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public DataEnvelope<List<ReportSummary>> listReports() {
        List<ReportSummary> summaries = reportService.listReports().stream().map(ReportSummary::from).toList();
        return new DataEnvelope<>(summaries);
    }

    @GetMapping("/{reportId}/metadata")
    public ResponseEntity<DataEnvelope<ReportMetadata>> getMetadata(
            @PathVariable String reportId,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        ReportDefinition definition = reportService.getDefinition(reportId);
        String etag = etagFor(definition);
        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build();
        }
        return ResponseEntity.ok().eTag(etag).body(new DataEnvelope<>(ReportMetadata.from(definition)));
    }

    @GetMapping("/{reportId}")
    public DataPageEnvelope<Map<String, Object>> getData(@PathVariable String reportId, ReportQuery query) {
        Page<ReportRow> page = reportService.getData(reportId, query);
        List<Map<String, Object>> data = page.content().stream().map(ReportRow::values).toList();
        return new DataPageEnvelope<>(data, PageInfo.from(page));
    }

    private static String etagFor(ReportDefinition definition) {
        // The schema is near-static, so a cheap hash of the definition is enough to
        // demonstrate cache awareness without maintaining a separate version field.
        return "\"" + Integer.toHexString(definition.hashCode()) + "\"";
    }
}
