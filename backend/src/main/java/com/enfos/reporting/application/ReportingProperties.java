package com.enfos.reporting.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "reporting")
public record ReportingProperties(
        @DefaultValue("25") int defaultPageSize,
        @DefaultValue("200") int maxPageSize
) {
}
