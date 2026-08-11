package com.enfos.reporting.infrastructure.reports;

import static org.assertj.core.api.Assertions.assertThat;

import com.enfos.reporting.application.ReportService;
import com.enfos.reporting.domain.model.ReportRow;
import com.enfos.reporting.domain.query.ReportQuery;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ReportSeedIntegrationTest {

    @Autowired
    private ReportService reportService;

    @Test
    void allThreeReportsLoadWithExpectedRowCounts() {
        assertThat(allRows("users")).hasSize(120);
        assertThat(allRows("departments")).hasSize(12);
        assertThat(allRows("projects")).hasSize(60);
    }

    @Test
    void everyProjectOwnerExistsInTheUsersReport() {
        Set<String> userNames = allRows("users").stream()
                .map(row -> (String) row.value("name"))
                .collect(Collectors.toSet());

        assertThat(allRows("projects"))
                .extracting(row -> (String) row.value("owner"))
                .allMatch(userNames::contains);
    }

    @Test
    void everyDepartmentManagerExistsInTheUsersReport() {
        Set<String> userNames = allRows("users").stream()
                .map(row -> (String) row.value("name"))
                .collect(Collectors.toSet());

        assertThat(allRows("departments"))
                .extracting(row -> (String) row.value("manager"))
                .allMatch(userNames::contains);
    }

    @Test
    void everyUserDepartmentExistsInTheDepartmentsReport() {
        Set<String> departmentNames = allRows("departments").stream()
                .map(row -> (String) row.value("name"))
                .collect(Collectors.toSet());

        assertThat(allRows("users"))
                .extracting(row -> (String) row.value("department"))
                .allMatch(departmentNames::contains);
    }

    private List<ReportRow> allRows(String reportId) {
        ReportQuery query = ReportQuery.of(null, List.of(), List.of(), 0, 200);
        return reportService.getData(reportId, query).content();
    }
}
