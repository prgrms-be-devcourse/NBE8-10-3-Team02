package com.back.domain.member.member.controller

import com.back.domain.member.member.dto.CheckNicknameResponse
import com.back.domain.member.member.dto.MemberMeResponse
import com.back.domain.member.member.dto.MemberNicknameChangeRequest
import com.back.domain.member.member.dto.MemberPasswordChangeRequest
import com.back.domain.member.member.service.MemberService
import com.back.global.exception.ServiceException
import com.back.global.rq.Rq
import com.back.global.rsData.RsData
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/members")
@Validated
class ApiV1MemberController(
    private val memberService: MemberService,
    private val rq: Rq,
) {
    @GetMapping("/check-nickname")
    fun checkNickname(
        @RequestParam
        @NotBlank(message = "닉네임은 필수 입력값입니다.")
        @Size(min = 2, max = 30, message = "닉네임은 2~30자여야 합니다.")
        nickname: String,
    ): RsData<CheckNicknameResponse> {
        val available = !memberService.existsByNickname(nickname)
        val message = if (available) "사용 가능한 닉네임입니다." else "이미 사용 중인 닉네임입니다."

        return RsData(
            "200-1",
            message,
            CheckNicknameResponse(available),
        )
    }

    @GetMapping("/me")
    fun me(): RsData<MemberMeResponse> {
        // rq.actor가 null이면 바로 예외 발생 (엘비스 연산자 사용)
        val actor = rq.actor ?: throw ServiceException("401-1", "로그인 후 이용해주세요.")

        val member = memberService.getMe(actor.id)
        return RsData("200-1", "내 정보 조회 성공", MemberMeResponse(member))
    }

    @PutMapping("/me/password")
    fun changePassword(
        @Valid @RequestBody req: MemberPasswordChangeRequest,
    ): RsData<Void?> {
        val actor = rq.actor ?: throw ServiceException("401-1", "로그인 후 이용해주세요.")

        memberService.changePassword(actor.id, req.oldPassword, req.newPassword)
        return RsData("200-1", "비밀번호 변경 성공")
    }

    @PutMapping("/me/nickname")
    fun changeNickname(
        @Valid @RequestBody req: MemberNicknameChangeRequest,
    ): RsData<Void?> {
        val actor = rq.actor ?: throw ServiceException("401-1", "로그인 후 이용해주세요.")

        memberService.changeNickname(actor.id, req.nickname)
        return RsData("200-1", "닉네임 변경 성공")
    }
}
