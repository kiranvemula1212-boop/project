package com.enfos.reporting.domain.query;

import com.enfos.reporting.domain.model.SortDirection;

public record SortSpec(String columnKey, SortDirection direction) {
}
