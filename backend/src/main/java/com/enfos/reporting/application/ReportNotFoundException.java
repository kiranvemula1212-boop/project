package com.enfos.reporting.application;

public final class ReportNotFoundException extends RuntimeException {

    private final String reportId;

    public ReportNotFoundException(String reportId) {
        super("No report found with id '" + reportId + "'.");
        this.reportId = reportId;
    }

    public String reportId() {
        return reportId;
    }
}
