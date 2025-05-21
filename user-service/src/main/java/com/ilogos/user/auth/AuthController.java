package com.ilogos.user.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ilogos.user.common.TokenInfo;
import com.ilogos.user.exception.ExceptionWithStatus;
import com.ilogos.user.jwt.JwtConfig;
import com.ilogos.user.jwt.JwtService;
import com.ilogos.user.response.ErrorResponse;
import com.ilogos.user.response.SuccessResponse;

import io.jsonwebtoken.ExpiredJwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
