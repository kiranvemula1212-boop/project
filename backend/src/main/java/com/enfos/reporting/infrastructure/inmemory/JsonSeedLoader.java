package com.enfos.reporting.infrastructure.inmemory;

import com.enfos.reporting.domain.model.ReportRow;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Loads a classpath JSON seed file — {@code { "lastUpdated": ..., "rows": [...] } }, one
 * file per report — into {@link SeedData}. Uses its own {@link ObjectMapper}, configured
 * with {@code USE_BIG_DECIMAL_FOR_FLOATS}, rather than the application's shared Jackson
 * bean — seed numbers must deserialize as Integer/Long/BigDecimal (never Double, which
 * would lose precision on currency values), and that setting has no business leaking
 * into how the rest of the app serializes JSON.
 */
@Component
public final class JsonSeedLoader {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);

    public SeedData load(String classpathLocation) {
        ClassPathResource resource = new ClassPathResource(classpathLocation);
        if (!resource.exists()) {
            throw new IllegalStateException("Seed file not found on the classpath: " + classpathLocation);
        }
        try (InputStream in = resource.getInputStream()) {
            RawSeedFile raw = objectMapper.readValue(in, RawSeedFile.class);
            if (raw.lastUpdated() == null || raw.rows() == null) {
                throw new IllegalStateException(
                        "Seed file must declare both 'lastUpdated' and 'rows': " + classpathLocation);
            }
            List<ReportRow> rows = raw.rows().stream().map(ReportRow::of).toList();
            return new SeedData(raw.lastUpdated(), rows);
        } catch (IOException e) {
            throw new IllegalStateException("Seed file is malformed: " + classpathLocation, e);
        }
    }

    public record SeedData(LocalDate lastUpdated, List<ReportRow> rows) {
    }

    private record RawSeedFile(LocalDate lastUpdated, List<Map<String, Object>> rows) {
    }
}
