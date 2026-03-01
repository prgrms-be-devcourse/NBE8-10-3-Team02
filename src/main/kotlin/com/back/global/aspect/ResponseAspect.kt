package com.back.global.aspect

import com.back.global.rsData.RsData
import jakarta.servlet.http.HttpServletResponse
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.stereotype.Component

@Aspect
@Component
@ConditionalOnWebApplication
class ResponseAspect(
    private val response: HttpServletResponse,
) {
    @Around(
        """
        execution(public com.back.global.rsData.RsData *(..)) &&
        (
            within(@org.springframework.stereotype.Controller *) ||
            within(@org.springframework.web.bind.annotation.RestController *)
        ) &&
        (
            @annotation(org.springframework.web.bind.annotation.GetMapping) ||
            @annotation(org.springframework.web.bind.annotation.PostMapping) ||
            @annotation(org.springframework.web.bind.annotation.PutMapping) ||
            @annotation(org.springframework.web.bind.annotation.DeleteMapping) ||
            @annotation(org.springframework.web.bind.annotation.RequestMapping)
        )
        """,
    )
    fun handleResponse(joinPoint: ProceedingJoinPoint): Any? {
        val proceed = joinPoint.proceed()

        // 결과가 RsData인 경우 상태 코드 동기화
        if (proceed is RsData<*>) {
            response.status = proceed.statusCode
        }

        return proceed
    }
}
