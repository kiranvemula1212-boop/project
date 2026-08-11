package com.enfos.reporting.api.dto;

import java.util.List;

/**
 * Wraps a paged collection response as {@code { "data": [...], "page": {...} } }. An
 * envelope rather than a bare array plus an {@code X-Total-Count} header — headers get
 * stripped by proxies and are awkward to consume from JS.
 */
public record DataPageEnvelope<T>(List<T> data, PageInfo page) {
}
