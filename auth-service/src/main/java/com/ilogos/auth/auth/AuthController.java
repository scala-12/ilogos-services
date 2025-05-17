package com.ilogos.auth.auth;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ilogos.auth.common.TokenInfo;
import com.ilogos.auth.exception.ExceptionWithStatus;
import com.ilogos.auth.jwt.JwtConfig;
import com.ilogos.auth.jwt.JwtService;
import com.ilogos.auth.response.ErrorResponse;
import com.ilogos.auth.response.SuccessResponse;

import io.jsonwebtoken.ExpiredJwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {

    private final JwtConfig jwtConfig;

    private record LoginRequest(
            String username,
            @Email String email,
            @Size(min = 3, max = 64) String password) {
    }

    private final JwtService jwtService;
    private final AuthService authService;

    @Operation(summary = "User login", description = "email/username & пароль, returns JWT tokens")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "401", description = "Invalid user data or blocked user", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<SuccessResponse<String>> login(
            @Valid @RequestBody LoginRequest req,
            HttpServletResponse response)
            throws NotFoundException {
        if (req.email == null && req.username == null) {
            log.error("login: Username not provided");
            throw new ExceptionWithStatus(HttpStatus.UNAUTHORIZED, "Username not provided");
        }

        String usernameOrEmail = req.email == null
                ? req.username
                : req.email;

        var tokens = authService.authenticate(usernameOrEmail, req.password)
                .orElseThrow(() -> new ExceptionWithStatus(HttpStatus.UNAUTHORIZED,
                        "Unable to log in with the provided data"));

        ResponseCookie cookie = ResponseCookie.from("refresh_token", tokens.refreshToken())
                .httpOnly(true)
                // TODO: make as env variable
                // .secure(true) // включить если HTTPS
                .path("/api/auth/refresh")
                .maxAge(jwtConfig.getRefreshTokenExpiration())
                .sameSite("Strict")
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return SuccessResponse.response(tokens.accessToken());
    }

    @Operation(summary = "Update access token", description = "Use with refresh token in cookies")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success refreshing"),
            @ApiResponse(responseCode = "400", description = "Refresh token invalid", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Refresh token expired", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<SuccessResponse<String>> refreshToken(
            @CookieValue(name = "refresh_token", required = true) String refreshToken) {
        TokenInfo tokenInfo;
        try {
            tokenInfo = jwtService.getTokenInfo(refreshToken);
        } catch (ExpiredJwtException ex) {
            throw new ExceptionWithStatus(HttpStatus.UNAUTHORIZED, ex);
        }
        if (!tokenInfo.isRefreshToken()) {
            throw new ExceptionWithStatus(HttpStatus.BAD_REQUEST,
                    "Used access-token instead of refresh-token");
        }

        var tokens = authService.refreshJwtToken(tokenInfo)
                .orElseThrow(() -> new ExceptionWithStatus(HttpStatus.UNAUTHORIZED,
                        "JWT refresh failed"));

        return SuccessResponse.response(tokens.accessToken());
    }

    @Operation(summary = "Validate access token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Valid token"),
            @ApiResponse(responseCode = "400", description = "Invalid token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Expired token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/validate")
    public ResponseEntity<SuccessResponse<?>> validateToken(
            @RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            throw new ExceptionWithStatus(HttpStatus.BAD_REQUEST);
        }

        TokenInfo tokenInfo;
        try {
            tokenInfo = jwtService.extractTokenInfoFromHeader(authHeader);
        } catch (ExpiredJwtException ex) {
            throw new ExceptionWithStatus(HttpStatus.UNAUTHORIZED, ex);
        }
        if (!tokenInfo.isAccessToken()) {
            throw new ExceptionWithStatus(HttpStatus.BAD_REQUEST,
                    "Used refresh-token instead of access-token");
        }
        log.info("Is valid token for %s".formatted(tokenInfo.getId()));

        return SuccessResponse.response();
    }

}
