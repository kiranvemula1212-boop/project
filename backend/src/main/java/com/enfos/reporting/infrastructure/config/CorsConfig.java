package com.enfos.reporting.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS is only needed for local development, where the Vite dev server (5173) calls the
 * backend (8080) across origins. In production, Nginx proxies /api on the same origin as
 * the built frontend, so no CORS configuration — and no environment-specific API URL —
 * is needed there at all.
 */
@Configuration
@Profile("dev")
class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET");
    }
}
