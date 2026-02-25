package com.back.domain.tag.tag.controller

import com.back.domain.tag.tag.dto.CreateTagRequest
import com.back.domain.tag.tag.dto.TagDto
import com.back.domain.tag.tag.service.TagService
import com.back.global.exception.ServiceException
import com.back.global.rsData.RsData
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/tags")
class TagController(
    private val tagService: TagService,
) {
    @GetMapping
    fun tags(): List<TagDto> = tagService.findAll().map { TagDto(it) }

    @PostMapping
    fun create(
        @RequestBody req: CreateTagRequest,
    ): RsData<TagDto> {
        val tag = tagService.create(req.content)
        return RsData("201-1", "태그가 생성되었습니다.", TagDto(tag))
    }

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: Int,
    ): RsData<Unit> {
        val tag =
            tagService
                .findById(id)
                .orElseThrow { ServiceException("404-1", "해당 태그를 찾을 수 없습니다.") }

        tagService.delete(tag)

        return RsData(
            "200-1",
            "${id}번 태그가 삭제되었습니다.",
        )
    }

    @PostMapping("/igdb/{igdbId}")
    fun createFromIgdb(
        @PathVariable igdbId: Long,
    ): RsData<List<TagDto>> {
        val tags = tagService.createTagsFromIgdb(igdbId)
        val tagDtos = tags.map { TagDto(it) }

        return RsData("201-2", "IGDB 정보로 태그가 생성되었습니다.", tagDtos)
    }
}
