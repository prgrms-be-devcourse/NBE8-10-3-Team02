package com.back.global.igdb

import com.back.global.exception.IgdbRetryableException
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.web.client.RestClient

/**
 * IgdbClient 단위 테스트
 * - 요청 헤더 (Client-ID, Authorization) 검증
 * - 요청 body (APICALYPSE) 검증
 * - 응답 JSON → DTO 역직렬화 검증
 *
 * 참고: RateLimiter는 AOP 기반으로 Spring 컨텍스트에서만 동작하므로 별도 통합 테스트 필요
 *       사용자 경로(IgdbRequestExecutor)는 Retry 없음. 배치 경로(BatchIgdbRequestExecutor)만 igdb-batch Retry 사용
 */
class IgdbClientTest {
    private val props =
        IgdbProperties(
            baseUrl = "http://placeholder",
            clientId = "test-client-id",
            clientSecret = "test-secret",
            tokenUrl = "http://placeholder",
        )
    private val tokenService = mock<TwitchTokenService>()

    private lateinit var server: MockWebServer
    private lateinit var igdbClient: IgdbClient

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

        val requestExecutor = IgdbRequestExecutor(restClient, props, tokenService)
        igdbClient = IgdbClient(requestExecutor)
    }

    @AfterEach
    fun tearDown() {
        server.close()
    }

    @Test
    fun `게임상세조회 - 성공`() {
        val body =
            """
            [
                {
                    "id": 999,
                    "name": "Test Game",
                    "summary": "Hello",
                    "first_release_date": 1645747200,
                    "cover": {"id": 10, "image_id": "co4jni"},
                    "involved_companies": [
                        {
                            "id": 1,
                            "company": {"id": 100, "name": "FromSoftware"},
                            "developer": true,
                            "publisher": false
                        },
                        {
                            "id": 2,
                            "company": {"id": 200, "name": "Bandai Namco"},
                            "developer": false,
                            "publisher": true
                        }
                    ],
                    "genres": [
                        { "id": 1, "name": "RPG" },
                        { "id": 2, "name": "Action" }
                    ],
                    "platforms": [{"id": 6, "name": "PC (Microsoft Windows)"}]
                }
            ]
            """.trimIndent()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(body),
        )

        val detail = igdbClient.getGameDetail(999L)
        assertThat(detail).isNotNull()
        assertThat(detail!!.id).isEqualTo(999L)
        assertThat(detail.name).isEqualTo("Test Game")

        // cover.image_id
        assertThat(detail.cover).isNotNull()
        assertThat(detail.cover!!.imageId).isEqualTo("co4jni")

        // involved_companies
        assertThat(detail.involvedCompanies).hasSize(2)
        // 개발사 검증
        assertThat(detail.involvedCompanies!!)
            .filteredOn { it.developer == true }
            .extracting<String?> { it.company?.name }
            .containsExactly("FromSoftware")
        // 퍼블리셔 검증
        assertThat(detail.involvedCompanies)
            .filteredOn { it.publisher == true }
            .extracting<String?> { it.company?.name }
            .containsExactly("Bandai Namco")

        // genres / platforms
        assertThat(detail.genres!!)
            .extracting<String?> { it.name }
            .containsExactlyInAnyOrder("RPG", "Action")
        assertThat(detail.platforms!!)
            .extracting<String?> { it.name }
            .contains("PC (Microsoft Windows)")

        val req = server.takeRequest()
        assertCommonRequest(req, "/games")

        val reqBody = req.body.readUtf8()
        assertThat(reqBody).contains("fields")
        assertThat(reqBody).contains("cover.id,cover.image_id")
        assertThat(reqBody).contains("where id = 999;")
        assertThat(reqBody).contains("limit 1;")
    }

    @Test
    fun `게임상세조회 - 빈 배열 반환 시 null 리턴`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("[]"),
        )

        assertThat(igdbClient.getGameDetail(1L)).isNull()
    }

    @Test
    fun `게임상세조회 - 500 예외`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"),
        )

        assertThatThrownBy { igdbClient.getGameDetail(1L) }
            .isInstanceOf(IgdbRetryableException::class.java)
    }

    @Test
    fun `영상Id조회 - 성공`() {
        val body =
            """
            [
                {
                    "id": 999,
                    "video_id": "VIDEO_ID"
                }
            ]
            """.trimIndent()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(body),
        )

        val igdbVideoDto = igdbClient.getVideoId(999L)
        assertThat(igdbVideoDto).isNotNull()
        assertThat(igdbVideoDto!!.id).isEqualTo(999L)
        assertThat(igdbVideoDto.videoId).isEqualTo("VIDEO_ID")

        val req = server.takeRequest()
        assertCommonRequest(req, "/game_videos")

        val reqBody = req.body.readUtf8()
        assertThat(reqBody).contains("fields")
        assertThat(reqBody).contains("video_id;")
        assertThat(reqBody).contains("where game = 999;")
        assertThat(reqBody).contains("sort id desc;")
        assertThat(reqBody).contains("limit 1;")
    }

    @Test
    fun `영상Id조회 - 빈 배열 반환 시 null 리턴`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("[]"),
        )

        assertThat(igdbClient.getVideoId(1L)).isNull()
    }

    @Test
    fun `비슷한게임조회 - 성공`() {
        val body =
            """
            [
                {
                    "id": 999,
                    "similar_games": [1,2,3,4,5]
                }
            ]
            """.trimIndent()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(body),
        )

        val similarGameIds = igdbClient.getSimilarGameIds(999L)
        assertThat(similarGameIds).containsExactly(1L, 2L, 3L, 4L, 5L)

        val req = server.takeRequest()
        assertCommonRequest(req, "/games")

        val reqBody = req.body.readUtf8()
        assertThat(reqBody).contains("fields")
        assertThat(reqBody).contains("similar_games;")
        assertThat(reqBody).contains("where id = 999;")
        assertThat(reqBody).contains("limit 1;")
    }

    @Test
    fun `비슷한게임조회 - 빈 배열 반환 시 빈 배열 리턴`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("[]"),
        )

        assertThat(igdbClient.getSimilarGameIds(1L)).isEmpty()
    }

    @Test
    fun `postReq - 요청 헤더에 Client-ID와 Authorization이 포함`() {
        val body =
            """
            [
                {
                    "id": 1,
                    "name": "Test"
                }
            ]
            """.trimIndent()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(body),
        )

        igdbClient.getGameDetail(1L)

        val req = server.takeRequest()
        assertThat(req.method).isEqualTo("POST")
        assertThat(req.getHeader("Client-ID")).isEqualTo("test-client-id")
        assertThat(req.getHeader("Authorization")).isEqualTo("Bearer test-access-token")
        assertThat(req.getHeader("Content-Type")).startsWith("text/plain")
    }

    private fun assertCommonRequest(
        req: okhttp3.mockwebserver.RecordedRequest,
        path: String,
    ) {
        assertThat(req.method).isEqualTo("POST")
        assertThat(req.path).isEqualTo(path)
        assertThat(req.getHeader("Client-ID")).isEqualTo("test-client-id")
        assertThat(req.getHeader("Authorization")).isEqualTo("Bearer test-access-token")
        assertThat(req.getHeader("Content-Type")).startsWith("text/plain")
    }
}
