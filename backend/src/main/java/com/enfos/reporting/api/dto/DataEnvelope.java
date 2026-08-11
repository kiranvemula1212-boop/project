package com.enfos.reporting.api.dto;

/** Wraps a single-resource response as {@code { "data": ... }}. */
public record DataEnvelope<T>(T data) {
}
