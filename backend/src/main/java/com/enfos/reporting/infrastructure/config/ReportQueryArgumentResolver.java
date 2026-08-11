package com.enfos.reporting.infrastructure.config;

import com.enfos.reporting.application.InvalidQueryException;
import com.enfos.reporting.application.ReportingProperties;
import com.enfos.reporting.domain.model.SortDirection;
import com.enfos.reporting.domain.query.FilterCriterion;
import com.enfos.reporting.domain.query.ReportQuery;
import com.enfos.reporting.domain.query.SortSpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.util.UriUtils;

/**
 * Builds a {@link ReportQuery} from raw request parameters, kept out of the controller so
 * the controller reads as three short methods. Malformed input (an unparsable page/size,
 * a sort with a bad direction) raises {@link InvalidQueryException} here — a 400, never a
 * 500 — because it is caught before the query ever reaches validation or a data source.
 */
@Component
class ReportQueryArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String FILTER_PREFIX = "filter.";

    private final ReportingProperties properties;

    ReportQueryArgumentResolver(ReportingProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(ReportQuery.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
        int page = parseIntOrDefault(webRequest.getParameter("page"), 0, "page");
        int size = parseIntOrDefault(webRequest.getParameter("size"), properties.defaultPageSize(), "size");
        String search = webRequest.getParameter("search");

        List<SortSpec> sorts = parseSorts(webRequest.getParameterValues("sort"));
        List<FilterCriterion> filters = parseFilters(webRequest.getParameterMap());

        return ReportQuery.of(search, filters, sorts, page, size);
    }

    private static int parseIntOrDefault(String raw, int fallback, String paramName) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new InvalidQueryException(List.of("'" + raw + "' is not a valid " + paramName + "."));
        }
    }

    private static List<SortSpec> parseSorts(String[] raw) {
        if (raw == null) {
            return List.of();
        }
        List<SortSpec> sorts = new ArrayList<>();
        for (String value : raw) {
            sorts.add(parseSort(value));
        }
        return sorts;
    }

    private static SortSpec parseSort(String value) {
        String[] parts = value.split(",", -1);
        if (parts.length > 2 || parts[0].isBlank()) {
            throw new InvalidQueryException(List.of("Malformed sort parameter: '" + value + "'."));
        }
        String columnKey = parts[0].trim();
        if (parts.length == 1 || parts[1].isBlank()) {
            return new SortSpec(columnKey, SortDirection.ASC);
        }
        String rawDirection = parts[1].trim();
        if (rawDirection.equalsIgnoreCase("asc")) {
            return new SortSpec(columnKey, SortDirection.ASC);
        }
        if (rawDirection.equalsIgnoreCase("desc")) {
            return new SortSpec(columnKey, SortDirection.DESC);
        }
        throw new InvalidQueryException(
                List.of("Malformed sort parameter: '" + value + "'. Direction must be 'asc' or 'desc'."));
    }

    private static List<FilterCriterion> parseFilters(Map<String, String[]> parameterMap) {
        List<FilterCriterion> filters = new ArrayList<>();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            if (!entry.getKey().startsWith(FILTER_PREFIX)) {
                continue;
            }
            String columnKey = entry.getKey().substring(FILTER_PREFIX.length());
            if (columnKey.isBlank()) {
                continue;
            }
            List<String> values = new ArrayList<>();
            for (String raw : entry.getValue()) {
                // Each value was percent-encoded (JS encodeURIComponent) before being
                // comma-joined by the client, specifically so a value containing a
                // literal comma — e.g. a "City, ST" location — survives the split
                // intact instead of being torn into two bogus values. UriUtils.decode,
                // not URLDecoder, because URLDecoder treats '+' as a space (form-encoding
                // semantics), which does not match encodeURIComponent's output.
                for (String v : raw.split(",", -1)) {
                    if (!v.isBlank()) {
                        values.add(UriUtils.decode(v, StandardCharsets.UTF_8));
                    }
                }
            }
            if (!values.isEmpty()) {
                filters.add(new FilterCriterion(columnKey, values));
            }
        }
        return filters;
    }
}
