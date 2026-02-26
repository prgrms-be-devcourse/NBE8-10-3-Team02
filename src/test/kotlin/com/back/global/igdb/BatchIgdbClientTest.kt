package com.back.global.igdb

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.web.client.RestClient

/**
 * BatchIgdbClient 단위 테스트
 * - fetchGamePage 요청 body (APICALYPSE) 검증
 * - updatedAfterEpoch 유무에 따른 where절 포함 여부 검증
 */
class BatchIgdbClientTest {
    private val props =
        IgdbProperties(
            baseUrl = "http://placeholder",
            clientId = "test-client-id",
            clientSecret = "test-secret",
            tokenUrl = "http://placeholder",
        )
    private val tokenService = mock<TwitchTokenService>()

    private lateinit var server: MockWebServer
    private lateinit var batchIgdbClient: BatchIgdbClient

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()

        val restClient =
            RestClient
                .builder()
                .baseUrl(server.url("/").toString())
                .build()

        whenever(tokenService.getAccessToken()).thenReturn("test-access-token")

        val requestExecutor = BatchIgdbRequestExecutor(restClient, props, tokenService)
        batchIgdbClient = BatchIgdbClient(requestExecutor)
    }

    @AfterEach
    fun tearDown() {
        server.close()
    }

    @Test
    fun `fetchGamePage - updatedAfterEpoch 없으면 where절 없이 요청`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("[]"),
        )

        batchIgdbClient.fetchGamePage(0, 500, null)

        val reqBody = server.takeRequest().body.readUtf8()
        assertThat(reqBody).doesNotContain("where updated_at")
        assertThat(reqBody).contains("offset 0;")
        assertThat(reqBody).contains("limit 500;")
    }

    @Test
    fun `fetchGamePage - updatedAfterEpoch 있으면 where updated_at 조건 포함`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("[]"),
        )

        batchIgdbClient.fetchGamePage(0, 500, 1700000000L)

        val reqBody = server.takeRequest().body.readUtf8()
        assertThat(reqBody).contains("where updated_at > 1700000000;")
        assertThat(reqBody).contains("offset 0;")
        assertThat(reqBody).contains("limit 500;")
    }

    @Test
    fun `fetchGamePage - 요청 헤더에 Client-ID와 Authorization이 포함`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("[]"),
        )

        batchIgdbClient.fetchGamePage(0, 500, null)

        val req = server.takeRequest()
        assertThat(req.method).isEqualTo("POST")
        assertThat(req.getHeader("Client-ID")).isEqualTo("test-client-id")
        assertThat(req.getHeader("Authorization")).isEqualTo("Bearer test-access-token")
        assertThat(req.getHeader("Content-Type")).startsWith("text/plain")
    }
}
