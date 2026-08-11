package com.enfos.reporting.infrastructure.inmemory;

import com.enfos.reporting.domain.model.ReportRow;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Loads a classpath JSON array of row objects into {@link ReportRow}s. Uses its own
 * {@link ObjectMapper}, configured with {@code USE_BIG_DECIMAL_FOR_FLOATS}, rather than
 * the application's shared Jackson bean — seed numbers must deserialize as
 * Integer/Long/BigDecimal (never Double, which would lose precision on currency values),
 * and that setting has no business leaking into how the rest of the app serializes JSON.
 */
@Component
public final class JsonSeedLoader {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);

    public List<ReportRow> load(String classpathLocation) {
        ClassPathResource resource = new ClassPathResource(classpathLocation);
        if (!resource.exists()) {
            throw new IllegalStateException("Seed file not found on the classpath: " + classpathLocation);
        }
        try (InputStream in = resource.getInputStream()) {
            List<Map<String, Object>> raw = objectMapper.readValue(in, new TypeReference<List<Map<String, Object>>>() {
            });
            return raw.stream().map(ReportRow::of).toList();
        } catch (IOException e) {
            throw new IllegalStateException("Seed file is malformed: " + classpathLocation, e);
        }
    }
}
