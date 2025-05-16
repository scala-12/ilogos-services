package com.ilogos.course.exception

import com.ilogos.course.response.ErrorResponse
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.security.SignatureException
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ExceptionController {

    private val log = LoggerFactory.getLogger(ExceptionController::class.java)

    @ExceptionHandler(ExceptionWithStatus::class)
    fun handleExceptionWithStatus(ex: ExceptionWithStatus) =
        ErrorResponse.response(ex.status, ex.message)

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(
        ex: ConstraintViolationException
    ): ResponseEntity<ErrorResponse> {
        val errors = ex.constraintViolations.map { "${it.propertyPath}: ${it.message}" }
        return ErrorResponse.response(HttpStatus.BAD_REQUEST, errors)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(
        ex: MethodArgumentNotValidException
    ): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }
        return ErrorResponse.response(HttpStatus.BAD_REQUEST, errors)
    }

    @ExceptionHandler(SignatureException::class)
    fun handleSignatureException(ex: SignatureException) =
        ErrorResponse.response(HttpStatus.BAD_REQUEST, ex)

    @ExceptionHandler(MalformedJwtException::class)
    fun handleMalformedJwtException() =
        ErrorResponse.response(HttpStatus.BAD_REQUEST, "Malformed JWT")

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unexpected error occurred", ex)
        return ErrorResponse.response(HttpStatus.INTERNAL_SERVER_ERROR, ex)
    }
}
