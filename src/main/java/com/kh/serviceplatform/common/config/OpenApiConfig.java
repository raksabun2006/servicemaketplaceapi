package com.kh.serviceplatform.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collections;

@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Service Marketplace Platform API")
                        .version("1.0.0")
                        .description("RESTful API documentation for Service Marketplace Platform.")
                        .contact(new Contact().name("API Support").email("support@serviceplatform.kh"))
                        .license(new License().name("Apache 2.0").url("https://springdoc.org")))
                .components(new Components()
                        .addSecuritySchemes(
                                SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT token (without 'Bearer ' prefix).")
                        ));
    }

    @Bean
    public OperationCustomizer customizeOperations() {
        return (operation, handlerMethod) -> {
            boolean hasClassPreAuth = handlerMethod.getBeanType().isAnnotationPresent(PreAuthorize.class);
            boolean hasMethodPreAuth = handlerMethod.hasMethodAnnotation(PreAuthorize.class);

            RequestMapping classMapping = handlerMethod.getBeanType().getAnnotation(RequestMapping.class);
            boolean isAuthEndpoint = false;
            if (classMapping != null) {
                for (String path : classMapping.value()) {
                    if (path.startsWith("/api/v1/auth")) {
                        isAuthEndpoint = true;
                        break;
                    }
                }
            }

            if (isAuthEndpoint) {
                operation.setSecurity(Collections.emptyList());
            } else if (hasClassPreAuth || hasMethodPreAuth) {
                operation.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
            }

            return operation;
        };
    }
}
