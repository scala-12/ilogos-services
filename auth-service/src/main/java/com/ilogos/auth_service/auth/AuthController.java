package com.ilogos.auth_service.auth;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ilogos.auth_service.exception.ExceptionWithStatus;
import com.ilogos.auth_service.response.ErrorResponse;
import com.ilogos.auth_service.response.SuccessResponse;
import com.ilogos.auth_service.user.RoleType;
import com.ilogos.auth_service.user.UserDTO;
import com.ilogos.auth_service.user.UserService;
import com.ilogos.auth_service.user.UserService.TokensData;
import com.ilogos.auth_service.utils.TokenInfo;
import com.ilogos.auth_service.validation.annotation.ValidTimezone;

import io.jsonwebtoken.ExpiredJwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {

        private record RegisterRequest(
                        @Size(min = 3, max = 64) String username,
                        @NotBlank @Email String email,
                        @Size(min = 6, max = 64) String password,
                        Optional<Boolean> isActive,
                        @NotEmpty List<RoleType> roles,
                        @ValidTimezone String timezone) {
        }

        private record LoginRequest(
                        String username,
                        @Email String email,
                        @Size(min = 3, max = 64) String password) {
        }

        private final UserService userService;
        private final JwtService jwtService;

        @Operation(summary = "User registration")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Success creation"),
                        @ApiResponse(responseCode = "400", description = "Invalid user data", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
        })
        @PostMapping("/register")
        public ResponseEntity<SuccessResponse<UserDTO>> register(@Valid @RequestBody RegisterRequest req) {
                var roles = req.roles.stream()
                                .filter(e -> !e.equals(RoleType.ROLE_ADMIN))
                                .collect(Collectors.toSet());
                if (roles.size() == 0 && req.roles.contains(RoleType.ROLE_ADMIN)) {
                        throw new ExceptionWithStatus(HttpStatus.BAD_REQUEST, "Administrator registration is denied");
                }
                var user = userService.create(
                                req.username,
                                req.email,
                                req.password,
                                req.isActive.orElseGet(() -> true),
                                roles,
                                req.timezone);

                return SuccessResponse.response(HttpStatus.CREATED, UserDTO.from(user));
        }

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

                String username = req.email == null
                                ? req.username
                                : req.email;

                var tokens = userService.authenticate(username, req.password)
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
