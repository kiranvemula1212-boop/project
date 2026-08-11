package com.enfos.reporting.application;

import java.util.List;

/**
 * Carries every violation found in a single {@code ReportQuery}, not just the first —
 * fixing one invalid parameter at a time and re-requesting is a bad experience.
 */
public final class InvalidQueryException extends RuntimeException {

    private final List<String> violations;

    public InvalidQueryException(List<String> violations) {
        super("Invalid query: " + String.join(" ", violations));
        this.violations = List.copyOf(violations);
    }

    public List<String> violations() {
        return violations;
    }
}
