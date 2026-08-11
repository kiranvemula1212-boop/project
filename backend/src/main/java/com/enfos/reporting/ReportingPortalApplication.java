package com.enfos.reporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ReportingPortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReportingPortalApplication.class, args);
    }
}
