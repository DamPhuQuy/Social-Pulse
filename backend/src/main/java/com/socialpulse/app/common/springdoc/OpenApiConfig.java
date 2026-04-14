package com.socialpulse.app.common.springdoc;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI(@Value("${app.version}") String appVersion) {
        var bearer = "bearer-key";
        var cookie = "cookie-key";
        return new OpenAPI()
                .info(new Info().title("API documentation for SocialPulse backend").version(appVersion)
                        .license(new License().name("Apache 2.0").url("https://springdoc.org"))
                        .description("JWT authentication using Bearer token"))
                .addSecurityItem(new SecurityRequirement().addList(bearer))
                .components(new Components()
                        // bearer
                        .addSecuritySchemes(bearer,
                            new SecurityScheme()
                                    .type(SecurityScheme.Type.HTTP)
                                    .scheme("bearer")
                                    .bearerFormat("JWT"))
                        // cookie
                        .addSecuritySchemes(cookie,
                                new SecurityScheme()
                                    .type(SecurityScheme.Type.APIKEY)
                                    .in(SecurityScheme.In.COOKIE)
                                    .bearerFormat("accessToken"))
                );
    }
}
