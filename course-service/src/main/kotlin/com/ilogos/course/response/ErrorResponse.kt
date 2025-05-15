package com.ilogos.course.response

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

class ErrorResponse(
        @Schema(description = "Errors list", example = "[\"Invalid credentials\"]")
        val errors: List<String> = emptyList()
) : AbstractResponse() {

    companion object {

        fun response(status: HttpStatus, errors: List<String>): ResponseEntity<ErrorResponse> {
            return ResponseEntity.status(status).body(ErrorResponse(errors))
        }

        fun response(status: HttpStatus, error: String?): ResponseEntity<ErrorResponse> {
            val errorList = if (error != null) listOf(error) else emptyList()
            return response(status, errorList)
        }

        fun response(status: HttpStatus, ex: Exception): ResponseEntity<ErrorResponse> {
            return response(status, ex.message)
        }
    }
}
