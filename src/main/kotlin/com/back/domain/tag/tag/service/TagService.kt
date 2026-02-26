package com.back.domain.tag.tag.service

import com.back.domain.tag.tag.entity.Tag
import com.back.domain.tag.tag.repository.TagRepository
import com.back.global.exception.ServiceException
import com.back.global.igdb.IgdbDefensiveClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

@Service
class TagService(
    private val tagRepository: TagRepository,
    private val igdbClient: IgdbDefensiveClient,
) {
    @Transactional
    fun create(content: String): Tag {
        tagRepository.findByContent(content).ifPresent {
            throw ServiceException("400-1", "이미 존재하는 태그입니다.")
        }

        return tagRepository.save(Tag(content))
    }

    fun findAll(): List<Tag> = tagRepository.findAll()

    fun findById(id: Int): Optional<Tag> = tagRepository.findById(id)

    fun delete(tag: Tag) {
        tagRepository.delete(tag)
    }

    /* fun modify(tag: Tag, content: String) {
        tag.content = content
    }
     */

    fun getOrCreate(content: String): Tag =
        tagRepository.findByContent(content).orElseGet {
            tagRepository.save(Tag(content))
        }

    // IGDB에서 게임 제목을 태그로 가져옴
    @Transactional
    fun createTagsFromIgdb(igdbId: Long): List<Tag> {
        val game =
            igdbClient.getGameName(igdbId)
                ?: throw ServiceException("404-3", "IGDB에서 정보를 찾을 수 없습니다.")

        val name =
            game.name
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: throw ServiceException("404-3", "IGDB 게임 이름이 비어있습니다.")

        return listOf(getOrCreate(name))
    }
}
