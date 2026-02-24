package com.back.domain.member.auth.controller

import com.back.domain.member.auth.dto.AuthLoginRequest
import com.back.domain.member.auth.dto.AuthLoginResponse
import com.back.domain.member.auth.dto.AuthSignupRequest
import com.back.domain.member.auth.dto.AuthSignupResponse
import com.back.domain.member.auth.dto.CheckEmailResponse
import com.back.domain.member.member.service.MemberService
import com.back.global.rq.Rq
import com.back.global.rsData.RsData
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
@Validated
class ApiV1AuthController(
    private val memberService: MemberService,
    private val rq: Rq,
) {
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody req: AuthLoginRequest,
    ): RsData<AuthLoginResponse> {
        val member = memberService.login(req.email, req.password)
        val accessToken = memberService.genAccessToken(member)

        // 프로퍼티 접근 (getApiKey() -> apiKey)
        rq.setCookie("apiKey", member.apiKey)
        rq.setCookie("accessToken", accessToken)

        return RsData(
            "200-1",
            "로그인 성공",
            AuthLoginResponse(member, member.apiKey, accessToken),
        )
    }

    @PostMapping("/signup")
    fun signup(
        @Valid @RequestBody req: AuthSignupRequest,
    ): RsData<AuthSignupResponse> {
        val member = memberService.join(req.email, req.password, req.nickname)

        return RsData(
            "201-1",
            "회원가입 성공",
            AuthSignupResponse(member),
        )
    }

    @PostMapping("/logout")
    fun logout(): RsData<Void?> {
        rq.deleteCookie("apiKey")
        rq.deleteCookie("accessToken")

        return RsData("200-1", "로그아웃 성공")
    }

    @GetMapping("/check-email")
    fun checkEmail(
        @RequestParam
        @NotBlank(message = "이메일은 필수 입력값입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        email: String,
    ): RsData<CheckEmailResponse> {
        val available = !memberService.existsByEmail(email)
        val message = if (available) "사용 가능한 이메일입니다." else "이미 사용 중인 이메일입니다."

        return RsData(
            "200-1",
            message,
            CheckEmailResponse(available),
        )
    }
}
