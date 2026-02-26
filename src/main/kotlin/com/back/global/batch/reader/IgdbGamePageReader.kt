package com.back.global.batch.reader

import com.back.global.igdb.BatchIgdbClient
import com.back.global.igdb.dto.IgdbGameDetailDto
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemStream
import org.springframework.batch.item.ItemStreamException

open class IgdbGamePageReader(
    private val igdbClient: BatchIgdbClient,
    private val updatedAfterEpoch: Long?,
) : ItemReader<IgdbGameDetailDto>,
    ItemStream {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val OFFSET_KEY = "igdb.game.offset"
        private const val PAGE_SIZE = 500
    }

    private var currentOffset = 0
    private val buffer = ArrayDeque<IgdbGameDetailDto>()
    private var exhausted = false

    init {
        if (updatedAfterEpoch != null) {
            log.info("증분 동기화 모드: updated_at > {} 이후 변경된 게임만 조회", updatedAfterEpoch)
        } else {
            log.info("전체 동기화 모드: 모든 게임 조회")
        }
    }

    @Throws(ItemStreamException::class)
    override fun open(executionContext: ExecutionContext) {
        if (executionContext.containsKey(OFFSET_KEY)) {
            currentOffset = executionContext.getInt(OFFSET_KEY)
            log.info("재시작 감지 - offset {}부터 재개", currentOffset)
        }
    }

    @Throws(ItemStreamException::class)
    override fun update(executionContext: ExecutionContext) {
        executionContext.putInt(OFFSET_KEY, currentOffset)
    }

    @Throws(ItemStreamException::class)
    override fun close() {
        buffer.clear()
    }

    /**
     * Spring Batch가 read() 호출
     *       ├─ buffer에 데이터 있음? → buffer에서 1건 반환
     *       ├─ buffer 비었는데 exhausted=true? → null 반환 (= Step 종료)
     *       └─ buffer 비었고 아직 데이터 남음?
     *            ├─ igdbClient.fetchGamePage(offset, 500, updatedAfterEpoch) 호출
     *            ├─ 결과가 빈 리스트 → exhausted=true, null 반환
     *            ├─ 결과가 500건 미만 → 마지막 페이지이므로 exhausted=true
     *            ├─ buffer에 전부 담음
     *            ├─ offset += 결과 건수
     *            └─ buffer에서 1건 반환
     */
    override fun read(): IgdbGameDetailDto? {
        if (buffer.isNotEmpty()) return buffer.removeFirst()
        if (exhausted) return null

        val page = igdbClient.fetchGamePage(currentOffset, PAGE_SIZE, updatedAfterEpoch)
        log.info("IGDB 게임 페이지 조회: offset={}, 결과={}건", currentOffset, page.size)

        if (page.isEmpty()) {
            exhausted = true
            return null
        }

        currentOffset += page.size
        buffer.addAll(page)

        if (page.size < PAGE_SIZE) {
            exhausted = true
        }

        return buffer.removeFirst()
    }
}
