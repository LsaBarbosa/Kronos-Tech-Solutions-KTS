package com.kts.kronos.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import org.springframework.context.annotation.Configuration;

import static com.kts.kronos.constants.Swagger.OPENAPI_DESCRIPTION;
import static com.kts.kronos.constants.Swagger.OPENAPI_SECURITY_BEARER_DESCRIPTION;
import static com.kts.kronos.constants.Swagger.OPENAPI_SECURITY_DESCRIPTION;
import static com.kts.kronos.constants.Swagger.OPENAPI_SECURITY_SCHEME;
import static com.kts.kronos.constants.Swagger.OPENAPI_SECURITY_SCHEME_BEARER;
import static com.kts.kronos.constants.Swagger.OPENAPI_TITLE;
import static com.kts.kronos.constants.Swagger.OPENAPI_VERSION;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = OPENAPI_TITLE,
                description = OPENAPI_DESCRIPTION,
                version = OPENAPI_VERSION
        ),
        security = @SecurityRequirement(name = OPENAPI_SECURITY_SCHEME)
)
@SecuritySchemes({
        @SecurityScheme(
                name = OPENAPI_SECURITY_SCHEME,
                type = SecuritySchemeType.APIKEY,
                in = io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.COOKIE,
                paramName = "APIKEY",
                description = OPENAPI_SECURITY_DESCRIPTION
        ),
        @SecurityScheme(
                name = OPENAPI_SECURITY_SCHEME_BEARER,
                type = SecuritySchemeType.HTTP,
                scheme = "bearer",
                bearerFormat = "JWT",
                description = OPENAPI_SECURITY_BEARER_DESCRIPTION
        )
})
public class OpenApiConfig {
}
