package com.logitrack.courierservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI shipmentServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LogiTrack - courier-service")
                        .version("1.0.0")
                        .description(
                                "API REST para la gestión de repartidores, " +
                                        "estado y disponibilidad en LogiTrack."
                        )
                        .contact(new Contact()
                                .name("Equipo LogiTrack")
                                .email("contacto@logitrack.com")));
    }
}
