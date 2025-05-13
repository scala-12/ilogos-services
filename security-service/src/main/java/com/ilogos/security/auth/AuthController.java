package com.ilogos.security.auth;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ilogos.security.exception.ExceptionWithStatus;
import com.ilogos.security.jwt.JwtService;
import com.ilogos.security.response.ErrorResponse;
import com.ilogos.security.response.SuccessResponse;
import com.ilogos.security.user.UserService;
import com.ilogos.security.user.UserService.TokensData;
import com.ilogos.security.utils.TokenInfo;

import io.jsonwebtoken.ExpiredJwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    private record LoginRequest(
            String username,
            @Email String email,
            @Size(min = 3, max = 64) String password) {
    }

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthService authService;

    @Operation(summary = "User login", description = "email/username & пароль, returns JWT tokens")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "401", description = "Invalid user data or blocked user", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<SuccessResponse<TokensData>> login(@Valid @RequestBody LoginRequest req)
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

        return SuccessResponse.response(tokens);
    }

    @Operation(summary = "Update access token", description = "Use with refresh token in header instead access token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success refreshing"),
            @ApiResponse(responseCode = "400", description = "Refresh token invalid", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Refresh token expired", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<SuccessResponse<TokensData>> refreshToken(
            @RequestHeader("Authorization") String authHeader) {
        TokenInfo tokenInfo;
        try {
            tokenInfo = jwtService.extractTokenInfoFromHeader(authHeader);
        } catch (ExpiredJwtException ex) {
            throw new ExceptionWithStatus(HttpStatus.UNAUTHORIZED, ex);
        }
        var tokens = userService.refreshUserToken(tokenInfo)
                .orElseThrow(() -> new ExceptionWithStatus(HttpStatus.UNAUTHORIZED,
                        "JWT refresh failed"));

        return SuccessResponse.response(tokens);
    }

}
