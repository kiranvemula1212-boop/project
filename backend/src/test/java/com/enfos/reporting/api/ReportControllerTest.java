package com.enfos.reporting.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.enfos.reporting.application.InvalidQueryException;
import com.enfos.reporting.application.ReportNotFoundException;
import com.enfos.reporting.application.ReportService;
import com.enfos.reporting.application.ReportingProperties;
import com.enfos.reporting.domain.model.ColumnDefinition;
import com.enfos.reporting.domain.model.ColumnType;
import com.enfos.reporting.domain.model.FilterType;
import com.enfos.reporting.domain.model.ReportDefinition;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(ReportController.class)
@EnableConfigurationProperties(ReportingProperties.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    private static final ColumnDefinition ID_COLUMN =
            new ColumnDefinition("id", "ID", ColumnType.ID, true, false, FilterType.NONE, List.of());
    private static final ReportDefinition USERS_DEFINITION =
            new ReportDefinition("users", "Users", "All users", "People", List.of(ID_COLUMN));

    @Test
    void listReportsReturnsExpectedShape() throws Exception {
        when(reportService.listReports()).thenReturn(List.of(USERS_DEFINITION));

        mockMvc.perform(get("/api/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("users"))
                .andExpect(jsonPath("$.data[0].name").value("Users"))
                .andExpect(jsonPath("$.data[0].columnCount").value(1));
    }

    @Test
    void unknownReportIdReturnsProblemJson404() throws Exception {
        when(reportService.getDefinition("nope")).thenThrow(new ReportNotFoundException("nope"));

        mockMvc.perform(get("/api/reports/nope/metadata"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.detail").value("No report found with id 'nope'."));
    }

    @Test
    void invalidSortColumnReturns400ListingTheViolation() throws Exception {
        when(reportService.getData(eq("users"), any()))
                .thenThrow(new InvalidQueryException(List.of("Unknown column 'nope' on report 'users'.")));

        mockMvc.perform(get("/api/reports/users").param("sort", "nope,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0]").value("Unknown column 'nope' on report 'users'."));
    }

    @Test
    void metadataReturnsAnEtagAndAMatchingIfNoneMatchReturns304() throws Exception {
        when(reportService.getDefinition("users")).thenReturn(USERS_DEFINITION);

        MvcResult first = mockMvc.perform(get("/api/reports/users/metadata"))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andReturn();
        String etag = first.getResponse().getHeader("ETag");

        mockMvc.perform(get("/api/reports/users/metadata").header("If-None-Match", etag))
                .andExpect(status().isNotModified());
    }
}
