package com.back.global.batch

import com.back.global.batch.reader.IgdbGamePageReader
import com.back.global.igdb.BatchIgdbClient
import com.back.support.IgdbFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class IgdbGamePageReaderTest {
    @Mock
    private lateinit var igdbClient: BatchIgdbClient

    @Test
    fun `전체 동기화 updatedAfterEpoch가 null이면 필터없이 호출한다`() {
        val reader = IgdbGamePageReader(igdbClient, null)
        whenever(igdbClient.fetchGamePage(0, 500, null)).thenReturn(listOf(IgdbFixtures.gameDetail(1L)))

        val result = reader.read()

        assertThat(result).isNotNull()
        assertThat(result!!.id).isEqualTo(1L)
        verify(igdbClient).fetchGamePage(0, 500, null)
    }

    @Test
    fun `증분 동기화 updatedAfterEpoch가 있으면 해당값으로 호출한다`() {
        val lastSync = 1700000000L
        val reader = IgdbGamePageReader(igdbClient, lastSync)
        whenever(igdbClient.fetchGamePage(0, 500, lastSync)).thenReturn(listOf(IgdbFixtures.gameDetail(1L)))

        val result = reader.read()

        assertThat(result).isNotNull()
        verify(igdbClient).fetchGamePage(0, 500, lastSync)
    }

    @Test
    fun `빈 페이지 반환시 null을 반환하고 종료한다`() {
        val reader = IgdbGamePageReader(igdbClient, null)
        whenever(igdbClient.fetchGamePage(0, 500, null)).thenReturn(emptyList())

        assertThat(reader.read()).isNull()
        assertThat(reader.read()).isNull() // 이후 호출도 null
    }

    @Test
    fun `페이지 크기보다 적으면 마지막 페이지로 판단한다`() {
        val reader = IgdbGamePageReader(igdbClient, null)
        whenever(igdbClient.fetchGamePage(0, 500, null))
            .thenReturn(listOf(IgdbFixtures.gameDetail(1L), IgdbFixtures.gameDetail(2L)))

        assertThat(reader.read()).isNotNull()
        assertThat(reader.read()).isNotNull()
        assertThat(reader.read()).isNull() // exhausted

        // 추가 API 호출 없음
        verify(igdbClient, times(1)).fetchGamePage(any(), any(), anyOrNull())
    }
}
