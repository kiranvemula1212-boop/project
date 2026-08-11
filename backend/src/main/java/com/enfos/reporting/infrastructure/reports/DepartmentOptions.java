package com.enfos.reporting.infrastructure.reports;

import com.enfos.reporting.domain.model.EnumOption;
import java.util.List;

/**
 * The 12 department names, shared by every report module that filters on department
 * (users, projects) so the filter options never drift out of sync with each other or with
 * the seed data. Department names are already human-readable, so value and label match.
 */
final class DepartmentOptions {

    static final List<EnumOption> ALL = List.of(
            new EnumOption("Engineering", "Engineering"),
            new EnumOption("Sales", "Sales"),
            new EnumOption("Marketing", "Marketing"),
            new EnumOption("Finance", "Finance"),
            new EnumOption("Human Resources", "Human Resources"),
            new EnumOption("Customer Success", "Customer Success"),
            new EnumOption("Product", "Product"),
            new EnumOption("Legal", "Legal"),
            new EnumOption("Operations", "Operations"),
            new EnumOption("IT", "IT"),
            new EnumOption("Design", "Design"),
            new EnumOption("Data & Analytics", "Data & Analytics")
    );

    private DepartmentOptions() {
    }
}
