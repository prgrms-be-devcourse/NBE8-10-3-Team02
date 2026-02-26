package com.back.global.igdb

import com.back.global.exception.IgdbRetryableException
import com.back.global.igdb.exception.IgdbApiException
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration
import java.util.concurrent.TimeUnit

class IgdbRequestExecutorTest {
    private lateinit var server: MockWebServer
    private lateinit var executor: IgdbRequestExecutor

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()

        val restClient =
            RestClient
                .builder()
                .baseUrl(server.url("/").toString())
                .build()

        val props =
            IgdbProperties(
                baseUrl = "http://placeholder",
                clientId = "test-client-id",
                clientSecret = "test-secret",
                tokenUrl = "http://placeholder",
            )
        val tokenService = mock<TwitchTokenService>()
        whenever(tokenService.getAccessToken()).thenReturn("test-token")

        executor = IgdbRequestExecutor(restClient, props, tokenService)
    }

    @AfterEach
    fun tearDown() {
        server.close()
    }

    @Test
    fun `200 응답 시 정상 반환`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("[{\"id\": 1}]"),
        )

        val result = executor.execute("test body", String::class.java, "/games", "testAction")

        assertThat(result).contains("id")
    }

    @Test
    fun `429 응답 시 IgdbRetryableException 발생`() {
        server.enqueue(MockResponse().setResponseCode(429))

        assertThatThrownBy { executor.execute("test body", String::class.java, "/games", "testAction") }
            .isInstanceOf(IgdbRetryableException::class.java)
    }

    @Test
    fun `500 응답 시 IgdbRetryableException 발생`() {
        server.enqueue(MockResponse().setResponseCode(500))

        assertThatThrownBy { executor.execute("test body", String::class.java, "/games", "testAction") }
            .isInstanceOf(IgdbRetryableException::class.java)
    }

    @Test
    fun `502 응답 시 IgdbRetryableException 발생`() {
        server.enqueue(MockResponse().setResponseCode(502))

        assertThatThrownBy { executor.execute("test body", String::class.java, "/games", "testAction") }
            .isInstanceOf(IgdbRetryableException::class.java)
    }

    @Test
    fun `503 응답 시 IgdbRetryableException 발생`() {
        server.enqueue(MockResponse().setResponseCode(503))

        assertThatThrownBy { executor.execute("test body", String::class.java, "/games", "testAction") }
            .isInstanceOf(IgdbRetryableException::class.java)
    }

    @Test
    fun `400 응답 시 IgdbApiException 발생 (재시도 대상 아님)`() {
        server.enqueue(MockResponse().setResponseCode(400).setBody("bad request"))

        assertThatThrownBy { executor.execute("test body", String::class.java, "/games", "testAction") }
            .isInstanceOf(IgdbApiException::class.java)
            .hasMessageContaining("testAction")
    }

    @Test
    fun `네트워크 타임아웃 시 ResourceAccessException 발생`() {
        val httpClient =
            HttpClient
                .newBuilder()
                .connectTimeout(Duration.ofSeconds(1))
                .build()
        val factory = JdkClientHttpRequestFactory(httpClient)
        factory.setReadTimeout(Duration.ofSeconds(1))

        val timeoutRestClient =
            RestClient
                .builder()
                .baseUrl(server.url("/").toString())
                .requestFactory(factory)
                .build()

        val props =
            IgdbProperties(
                baseUrl = "http://placeholder",
                clientId = "test-client-id",
                clientSecret = "test-secret",
                tokenUrl = "http://placeholder",
            )
        val tokenService = mock<TwitchTokenService>()
        whenever(tokenService.getAccessToken()).thenReturn("test-token")

        val timeoutExecutor = IgdbRequestExecutor(timeoutRestClient, props, tokenService)

        server.enqueue(
            MockResponse()
                .setBody("{}")
                .setHeadersDelay(3, TimeUnit.SECONDS),
        )

        assertThatThrownBy { timeoutExecutor.execute("test body", String::class.java, "/games", "testAction") }
            .isInstanceOf(ResourceAccessException::class.java)
    }
}
