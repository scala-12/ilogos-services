package com.ilogos.course.config.docs

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun customOpenAPI(): OpenAPI = OpenAPI().info(
        Info().title("Auth Service API")
            .version("1.0")
            .description("Documentation for iLogos authorization service")
    )
}
