package com.enfos.reporting.domain.model;

/**
 * Drives which filter control the client shows. Deliberately excludes DATE_RANGE and
 * NUMBER_RANGE — we do not advertise filter capabilities that have not actually been built.
 */
public enum FilterType {
    NONE,
    TEXT,
    ENUM
}
