package com.ilogos.auth.user;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ilogos.auth.common.TokenInfo;
import com.ilogos.auth.common.validation.annotation.ValidTimezone;
import com.ilogos.auth.exception.ExceptionWithStatus;
import com.ilogos.auth.jwt.JwtService;
import com.ilogos.auth.response.ErrorResponse;
import com.ilogos.auth.response.SuccessResponse;
import com.ilogos.auth.user.model.RoleType;
import com.ilogos.auth.user.model.UserDTO;

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
@RequestMapping("/api/user")
@AllArgsConstructor
public class UserController {

        private final UserService userService;
        private final JwtService jwtService;

        public record UpdateUserRequest(
                        Optional<String> email,
                        Optional<String> password,
                        Optional<String> username) {
        }

        private record RegisterRequest(
                        @Size(min = 3, max = 64) String username,
                        @NotBlank @Email String email,
                        @Size(min = 6, max = 64) String password,
                        Optional<Boolean> isActive,
                        @NotEmpty List<RoleType> roles,
                        @ValidTimezone String timezone) {
        }

        @Operation(summary = "Update user data", description = "The user ID is extracted from the access token")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Success update"),
                        @ApiResponse(responseCode = "400", description = "Refresh token invalid", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
                        @ApiResponse(responseCode = "403", description = "User not exists", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
        })
        @PutMapping("/update")
        public ResponseEntity<SuccessResponse<UserDTO>> updateUser(@RequestBody UpdateUserRequest request,
                        @RequestHeader("Authorization") String authHeader) {
                TokenInfo tokenInfo;
                try {
                        tokenInfo = jwtService.extractTokenInfoFromHeader(authHeader);
                } catch (ExpiredJwtException ex) {
                        throw new ExceptionWithStatus(HttpStatus.UNAUTHORIZED, ex);
                }
                var user = userService.updateSelf(tokenInfo, request).map(UserDTO::from);
                return user.map(e -> SuccessResponse.response(e))
                                .orElseThrow(() -> new ExceptionWithStatus(HttpStatus.FORBIDDEN));
        }

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

}
