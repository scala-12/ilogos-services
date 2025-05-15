package com.ilogos.course.response

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

class SuccessResponse<T>(@Schema(description = "Response data") val data: T?) : AbstractResponse() {

    companion object {
        fun response(): ResponseEntity<SuccessResponse<Any>> {
            return ResponseEntity.status(HttpStatus.OK).body(SuccessResponse<Any>(null))
        }

        fun <T> response(data: T): ResponseEntity<SuccessResponse<T>> {
            return ResponseEntity.status(HttpStatus.OK).body(SuccessResponse(data))
        }

        fun <T> response(status: HttpStatus, data: T): ResponseEntity<SuccessResponse<T>> {
            return ResponseEntity.status(status).body(SuccessResponse(data))
        }
    }
}
