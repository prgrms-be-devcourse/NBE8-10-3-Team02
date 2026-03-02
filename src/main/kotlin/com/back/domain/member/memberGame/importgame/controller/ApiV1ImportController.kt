package com.back.domain.member.memberGame.importgame.controller

import com.back.domain.member.memberGame.importgame.dto.ImportConfirmRequest
import com.back.domain.member.memberGame.importgame.dto.ImportConfirmResponse
import com.back.domain.member.memberGame.importgame.dto.ImportJobStatus
import com.back.domain.member.memberGame.importgame.dto.ImportMatchRequest
import com.back.domain.member.memberGame.importgame.service.ImportService
import com.back.global.exception.ServiceException
import com.back.global.rq.Rq
import com.back.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/members/{memberId}/library/import")
@Tag(name = "Import", description = "게임 라이브러리 일괄 가져오기")
class ApiV1ImportController(
    private val importService: ImportService,
    private val rq: Rq,
) {
    @PostMapping("/match")
    @Operation(summary = "게임 이름 매칭 시작 (비동기)")
    fun startMatch(
        @PathVariable memberId: Int,
        @Valid @RequestBody request: ImportMatchRequest,
    ): RsData<Map<String, String>> {
        val actor = rq.actor ?: throw ServiceException("401-1", "로그인이 필요합니다.")
        if (memberId != actor.id) throw ServiceException("403", "Cannot import to this library")

        val jobId = importService.matchGamesAsync(memberId, request)
        return RsData("200-1", "매칭 작업이 시작되었습니다.", mapOf("jobId" to jobId))
    }

    @GetMapping("/match/{jobId}")
    @Operation(summary = "매칭 작업 상태 조회 (폴링)")
    fun pollJob(
        @PathVariable memberId: Int,
        @PathVariable jobId: String,
    ): RsData<ImportJobStatus> {
        val actor = rq.actor ?: throw ServiceException("401-1", "로그인이 필요합니다.")
        if (memberId != actor.id) throw ServiceException("403", "Cannot view this job")

        val status = importService.getJobStatus(jobId)
            ?: throw ServiceException("404", "Job not found: $jobId")
        return RsData("200-1", "작업 상태 조회", status)
    }

    @PostMapping("/confirm")
    @Operation(summary = "매칭 결과 확인 후 일괄 추가")
    fun confirmImport(
        @PathVariable memberId: Int,
        @Valid @RequestBody request: ImportConfirmRequest,
    ): RsData<ImportConfirmResponse> {
        val actor = rq.actor ?: throw ServiceException("401-1", "로그인이 필요합니다.")
        if (memberId != actor.id) throw ServiceException("403", "Cannot import to this library")

        val response = importService.confirmImport(memberId, request)
        return RsData("200-1", "가져오기가 완료되었습니다.", response)
    }
}
