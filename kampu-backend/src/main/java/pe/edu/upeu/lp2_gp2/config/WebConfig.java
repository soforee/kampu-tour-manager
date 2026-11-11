package pe.edu.upeu.lp2_gp2.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import pe.edu.upeu.lp2_gp2.seguridad.SeguridadInterceptor;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${storage.location}")
    private String storageLocation;

    private final SeguridadInterceptor seguridad;

    public WebConfig(SeguridadInterceptor seguridad) {
        this.seguridad = seguridad;
    }

    // Seguridad por rol de la API (PKG_SEGURIDAD_ROLES)
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(seguridad).addPathPatterns("/api/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String carpeta = Paths.get(storageLocation).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/images/**")
                .addResourceLocations(carpeta.endsWith("/") ? carpeta : carpeta + "/");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:4200")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
