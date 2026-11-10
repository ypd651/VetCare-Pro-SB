package com.veterinaria.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI veterinariaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("VetCare Pro - API Clínica Veterinaria")
                        .version("1.0.0")
                        .description("API REST para gestión integral de propietarios, pacientes, agenda de citas con control de conflictos, historial médico y alertas preventivas de vacunación.")
                        .contact(new Contact().name("Clínica Veterinaria VetCare")));
    }
}
