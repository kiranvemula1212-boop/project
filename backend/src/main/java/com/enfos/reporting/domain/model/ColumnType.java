package com.enfos.reporting.domain.model;

/**
 * Drives client-side rendering only — how a column's values are displayed, not how they
 * are filtered. See {@link FilterType} for the filter-control counterpart.
 */
public enum ColumnType {
    ID,
    TEXT,
    EMAIL,
    NUMBER,
    CURRENCY,
    DATE,
    ENUM
}
