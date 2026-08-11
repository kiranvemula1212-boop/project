package com.enfos.reporting.infrastructure.config;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class WebConfig implements WebMvcConfigurer {

    private final ReportQueryArgumentResolver reportQueryArgumentResolver;

    WebConfig(ReportQueryArgumentResolver reportQueryArgumentResolver) {
        this.reportQueryArgumentResolver = reportQueryArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(reportQueryArgumentResolver);
    }
}
