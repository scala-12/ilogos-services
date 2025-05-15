package com.ilogos.course.response

import java.time.LocalDateTime

abstract class AbstractResponse {
    val timestamp: LocalDateTime = LocalDateTime.now()
}
