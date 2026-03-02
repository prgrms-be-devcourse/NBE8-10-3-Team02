package com.back.domain.member.member.service

import com.back.domain.member.auth.service.AuthTokenService
import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.repository.MemberRepository
import com.back.global.exception.ServiceException
import org.springframework.cache.annotation.Cacheable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MemberService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authTokenService: AuthTokenService,
) {
    fun findByEmail(email: String): Member? = memberRepository.findByEmail(email)

    @Cacheable(cacheNames = ["apiKeys"], key = "#apiKey")
    fun findByApiKey(apiKey: String): Member? = memberRepository.findByApiKey(apiKey)

    fun findById(id: Int): Member? = memberRepository.findById(id).orElse(null)

    fun existsByEmail(email: String): Boolean = memberRepository.existsByEmail(email)

    fun existsByNickname(nickname: String): Boolean = memberRepository.existsByNickname(nickname)

    @Transactional
    fun join(
        email: String,
        password: String,
        nickname: String,
    ): Member {
        if (memberRepository.existsByEmail(email)) {
            throw ServiceException("409-1", "이미 존재하는 이메일입니다.")
        }
        if (memberRepository.existsByNickname(nickname)) {
            throw ServiceException("409-2", "이미 존재하는 닉네임입니다.")
        }

        val encoded = passwordEncoder.encode(password)
        val member = Member(email, encoded, nickname)
        return memberRepository.save(member)
    }

    fun login(
        email: String,
        password: String,
    ): Member {
        val member =
            memberRepository.findByEmail(email)
                ?: throw ServiceException("401-1", "이메일 또는 비밀번호가 올바르지 않습니다.")

        if (!passwordEncoder.matches(password, member.password)) {
            throw ServiceException("401-1", "이메일 또는 비밀번호가 올바르지 않습니다.")
        }
        return member
    }

    fun genAccessToken(member: Member): String = authTokenService.genAccessToken(member)

    fun payload(accessToken: String): Map<String, Any> =
        authTokenService.payload(accessToken)
            ?: throw ServiceException("401-2", "인증 정보가 유효하지 않습니다.")

    @Transactional
    fun changePassword(
        memberId: Int,
        oldPassword: String,
        newPassword: String,
    ): Member {
        val member =
            memberRepository.findById(memberId).orElseThrow {
                ServiceException("404-1", "회원이 존재하지 않습니다.")
            }

        if (!passwordEncoder.matches(oldPassword, member.password)) {
            throw ServiceException("401-2", "현재 비밀번호가 일치하지 않습니다.")
        }

        if (passwordEncoder.matches(newPassword, member.password)) {
            throw ServiceException("400-2", "새 비밀번호는 기존 비밀번호와 달라야 합니다.")
        }

        val encoded = passwordEncoder.encode(newPassword)
        member.changePassword(encoded)
        return member
    }

    fun getMe(memberId: Int): Member =
        memberRepository.findById(memberId).orElseThrow {
            ServiceException("404-1", "회원이 존재하지 않습니다.")
        }

    @Transactional
    fun flush() {
        memberRepository.flush()
    }

    @Transactional
    fun changeNickname(
        memberId: Int,
        newNickname: String,
    ): Member {
        val member =
            memberRepository.findById(memberId).orElseThrow {
                ServiceException("404-1", "회원이 존재하지 않습니다.")
            }

        val nn = newNickname.trim()

        if (member.nickname == nn) {
            throw ServiceException("400-1", "현재 닉네임과 동일합니다.")
        }

        if (memberRepository.existsByNicknameAndIdNot(nn, memberId)) {
            throw ServiceException("409-1", "이미 사용 중인 닉네임입니다.")
        }

        member.changeNickname(nn)
        return member
    }
}
