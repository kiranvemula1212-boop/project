package com.enfos.reporting.application;

import com.enfos.reporting.domain.model.ReportDefinition;
import com.enfos.reporting.domain.port.ReportModule;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Indexes every {@link ReportModule} Spring finds by its definition id. Fails fast at
 * startup on a duplicate id rather than at request time — startup failures are cheap,
 * production 500s are not.
 */
@Component
class ReportRegistry {

    private final Map<String, ReportModule> modulesById;

    ReportRegistry(List<ReportModule> modules) {
        // LinkedHashMap to preserve injection order, which in turn preserves the order
        // reports are declared in — useful for a stable, predictable listing endpoint.
        Map<String, ReportModule> byId = new LinkedHashMap<>();
        for (ReportModule module : modules) {
            String id = module.definition().id();
            ReportModule existing = byId.putIfAbsent(id, module);
            if (existing != null) {
                throw new IllegalStateException(
                        "Duplicate report id '" + id + "': registered by both "
                                + existing.getClass().getSimpleName() + " and "
                                + module.getClass().getSimpleName() + ".");
            }
        }
        this.modulesById = Collections.unmodifiableMap(byId);
    }

    List<ReportDefinition> definitions() {
        return modulesById.values().stream().map(ReportModule::definition).toList();
    }

    Optional<ReportModule> find(String reportId) {
        return Optional.ofNullable(modulesById.get(reportId));
    }
}
