package com.ventas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Configura qué sitios web pueden llamar a la API desde el navegador. */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /** Se configura con CORS_ORIGINS; se admiten varios orígenes separados por coma. */
    @Value("${app.cors.origins:*}")
    private String origenes;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Solo aplica a rutas API y a los métodos que usa este proyecto.
        registry.addMapping("/api/**")
                .allowedOriginPatterns(origenes.split(","))
                .allowedMethods("GET", "POST", "OPTIONS");
    }
}
