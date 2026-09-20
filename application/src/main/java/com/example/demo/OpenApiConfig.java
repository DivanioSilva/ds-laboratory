package com.example.demo;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI applicationOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Spring Boot Maven Demo API")
                        .description("Documentação da API REST de exemplo")
                        .version("1.0.0"));
    }
}
