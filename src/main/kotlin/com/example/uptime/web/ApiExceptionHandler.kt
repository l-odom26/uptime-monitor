package com.example.uptime.web

import com.example.uptime.service.InvalidTargetException
import com.example.uptime.service.MonitorNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

/** Maps domain exceptions to RFC 7807 problem responses; validation errors are handled by the base class. */
@RestControllerAdvice
class ApiExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(MonitorNotFoundException::class)
    fun notFound(e: MonitorNotFoundException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.message ?: "Not found")

    @ExceptionHandler(InvalidTargetException::class)
    fun invalidTarget(e: InvalidTargetException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.message ?: "Invalid target")
}
