package com.ilogos.auth_service.user;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ilogos.auth_service.auth.JwtService;
import com.ilogos.auth_service.exception.ExceptionWithStatus;
import com.ilogos.auth_service.response.ErrorResponse;
import com.ilogos.auth_service.response.SuccessResponse;
import com.ilogos.auth_service.utils.TokenInfo;

import io.jsonwebtoken.ExpiredJwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
        var user = userService.updateByAuth(tokenInfo, request).map(UserDTO::from);
        return user.map(e -> SuccessResponse.response(e))
                .orElseThrow(() -> new ExceptionWithStatus(HttpStatus.FORBIDDEN));
    }

}
