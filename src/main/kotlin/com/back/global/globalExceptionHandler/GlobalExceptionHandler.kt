package com.back.global.globalExceptionHandler

import com.back.global.exception.ServiceException
import com.back.global.rsData.RsData
import com.back.global.steam.exception.SteamApiException
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(NoSuchElementException::class)
    fun handle(ex: NoSuchElementException): ResponseEntity<RsData<Void>> =
        ResponseEntity(
            RsData("404-1", "해당 데이터가 존재하지 않습니다."),
            HttpStatus.NOT_FOUND,
        )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handle(ex: MethodArgumentNotValidException): ResponseEntity<RsData<Void>> {
        // 자바 스트림 대신 joinToString 사용
        val message =
            ex.bindingResult.fieldErrors
                .sortedBy { it.field }
                .joinToString("\n") { it.defaultMessage ?: "" }

        return ResponseEntity(
            RsData("400-1", message),
            HttpStatus.BAD_REQUEST,
        )
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handle(ex: ConstraintViolationException): ResponseEntity<RsData<Void>> {
        val message =
            ex.constraintViolations
                .sortedBy { it.propertyPath.toString() }
                .joinToString("\n") { it.message }

        return ResponseEntity(
            RsData("400-1", message),
            HttpStatus.BAD_REQUEST,
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handle(ex: HttpMessageNotReadableException): ResponseEntity<RsData<Void>> =
        ResponseEntity(
            RsData("400-1", "요청 본문이 올바르지 않습니다."),
            HttpStatus.BAD_REQUEST,
        )

    @ExceptionHandler(MissingRequestHeaderException::class)
    fun handle(ex: MissingRequestHeaderException): ResponseEntity<RsData<Void>> =
        ResponseEntity(
            RsData("400-1", "필수 헤더가 누락되었습니다: ${ex.headerName}"),
            HttpStatus.BAD_REQUEST,
        )

    @ExceptionHandler(SteamApiException::class)
    fun handle(ex: SteamApiException): ResponseEntity<RsData<Void>> =
        ResponseEntity(
            RsData("503-1", "Steam 서비스 연결에 실패했습니다."),
            HttpStatus.SERVICE_UNAVAILABLE,
        )

    @ExceptionHandler(ServiceException::class)
    fun handle(
        ex: ServiceException,
        response: HttpServletResponse,
    ): RsData<Void> {
        val rsData = ex.rsData
        response.status = rsData.statusCode // RsData에 정의된 상태 코드를 응답에 설정
        return rsData
    }
}
