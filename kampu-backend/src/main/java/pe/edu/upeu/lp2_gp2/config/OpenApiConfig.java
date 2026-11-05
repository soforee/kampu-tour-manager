package pe.edu.upeu.lp2_gp2.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("KAMPU Tour Manager - API")
                        .version("1.0")
                        .description("API REST del sistema de reservas de tours."));
    }
}
