package pe.edu.upeu.lp2_gp2.seguridad;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class SesionOracleConfig {
    // Envuelve el DataSource que crea Spring Boot con SesionOracleDataSource
    @Bean
    public static BeanPostProcessor sesionOracleDataSourcePostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String nombre) {
                if (bean instanceof DataSource ds && !(bean instanceof SesionOracleDataSource)) {
                    return new SesionOracleDataSource(ds);
                }
                return bean;
            }
        };
    }
}
