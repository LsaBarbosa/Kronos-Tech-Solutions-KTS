package com.kts.kronos.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Kronos API",
                version = "1.0",
                description = "API para gerenciamento de funcionários de uma empresa",
            contact = @Contact(name = "Team Kronos",email = "kronos.solution.contact@gmail.com" )
        )
)
public class OpenApiConfig {
}
