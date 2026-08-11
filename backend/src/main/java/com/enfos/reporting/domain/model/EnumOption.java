package com.enfos.reporting.domain.model;

/**
 * One selectable value for an ENUM-filtered column: the raw value the API sends and
 * filters on, and the label the client displays.
 */
public record EnumOption(String value, String label) {

    public EnumOption {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("EnumOption value must not be blank.");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("EnumOption label must not be blank.");
        }
    }
}
