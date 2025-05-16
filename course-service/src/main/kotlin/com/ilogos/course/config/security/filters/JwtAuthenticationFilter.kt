package com.ilogos.course.config.security.filters

import com.ilogos.course.jwt.JwtService
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService
) : OncePerRequestFilter() {

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")

        val tokenInfo = jwtService.getTokenInfo(authHeader.substring(7))

        if (!tokenInfo.isAccess) {
            SecurityContextHolder.clearContext()
        }
        filterChain.doFilter(request, response)
    }
}
