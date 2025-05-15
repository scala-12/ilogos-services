package com.ilogos.course.exception

import org.springframework.http.HttpStatus

class ExceptionWithStatus(val status: HttpStatus, message: String?, cause: Throwable? = null) :
        RuntimeException(message, cause) {

    constructor(status: HttpStatus, ex: Exception) : this(status, ex.message, ex)

    constructor(status: HttpStatus, msg: String) : this(status, msg, null)
}
