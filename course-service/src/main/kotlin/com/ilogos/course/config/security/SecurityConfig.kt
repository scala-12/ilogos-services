package com.ilogos.course.config.security

import com.ilogos.course.config.security.filters.JwtAuthenticationFilter
import com.ilogos.course.jwt.JwtService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig(private val jwtService: JwtService, private val jwtAuthenticationFilter: JwtAuthenticationFilter) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val builder = http.authorizeHttpRequests({
            it.requestMatchers(
                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
            ).permitAll().anyRequest().authenticated()
        }).oauth2ResourceServer { it.jwt(Customizer.withDefaults()) }

        http.addFilterAfter(
            jwtAuthenticationFilter, BearerTokenAuthenticationFilter::class.java
        )

        return builder.build()
    }

    @Bean
    fun jwtDecoder() = jwtService.buildJwtDecoder()
}
