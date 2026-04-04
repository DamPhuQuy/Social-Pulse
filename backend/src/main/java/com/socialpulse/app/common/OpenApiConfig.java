package com.socialpulse.app.common;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình Swagger/OpenAPI.
 *
 * @SecurityScheme khai báo scheme "bearerAuth" kiểu Bearer token.
 * Khi có annotation này, Swagger UI sẽ hiển thị nút "Authorize 🔒"
 * cho phép nhập JWT token để test các route protected.
 */
@Configuration
@OpenAPIDefinition(info = @Info(title = "Social Pulse API", version = "v1"))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}
