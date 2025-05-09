package com.ilogos.auth_service.controller;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ilogos.auth_service.exceptions.ExceptionWithStatus;
import com.ilogos.auth_service.model.dto.UserDTO;
import com.ilogos.auth_service.model.response.SuccessResponse;
import com.ilogos.auth_service.service.UserService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/user")
@AllArgsConstructor
public class UserController {

        private final UserService userService;

        public record UpdateUserRequest(
                        Optional<String> email,
                        Optional<String> password,
                        Optional<String> username) {
        }

        @PutMapping("/update")
        public ResponseEntity<SuccessResponse<UserDTO>> updateUser(@RequestBody UpdateUserRequest request,
                        @RequestHeader("Authorization") String authHeader) {
                var user = userService.updateByAuth(authHeader, request).map(UserDTO::from);
                return user.map(e -> SuccessResponse.response(e))
                                .orElseThrow(() -> new ExceptionWithStatus(HttpStatus.FORBIDDEN));
        }

}
