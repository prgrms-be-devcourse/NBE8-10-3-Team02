package com.back.global.steam

import com.back.global.steam.exception.SteamApiException
import com.back.global.steam.exception.SteamRetryableException
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration
import java.util.concurrent.TimeUnit

class SteamRequestExecutorTest {
    private lateinit var server: MockWebServer
    private lateinit var executor: SteamRequestExecutor

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()

        val restClient =
            RestClient
                .builder()
                .baseUrl(server.url("/").toString())
                .build()

        val props = SteamProperties(baseUrl = "http://placeholder", apiKey = "test-api-key")
        executor = SteamRequestExecutor(restClient, props)
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
                .setBody("{\"response\":{\"game_count\":1,\"games\":[{\"appid\":10}]}}"),
        )

        val result =
            executor.execute(
                { it.path("/IPlayerService/GetOwnedGames/v1/").build() },
                String::class.java,
                "testAction",
            )

        assertThat(result).contains("appid")
    }

    @Test
    fun `429 응답 시 SteamRetryableException 발생`() {
        server.enqueue(MockResponse().setResponseCode(429))

        assertThatThrownBy {
            executor.execute({ it.path("/test").build() }, String::class.java, "testAction")
        }.isInstanceOf(SteamRetryableException::class.java)
    }

    @Test
    fun `500 응답 시 SteamRetryableException 발생`() {
        server.enqueue(MockResponse().setResponseCode(500))

        assertThatThrownBy {
            executor.execute({ it.path("/test").build() }, String::class.java, "testAction")
        }.isInstanceOf(SteamRetryableException::class.java)
    }

    @Test
    fun `502 응답 시 SteamRetryableException 발생`() {
        server.enqueue(MockResponse().setResponseCode(502))

        assertThatThrownBy {
            executor.execute({ it.path("/test").build() }, String::class.java, "testAction")
        }.isInstanceOf(SteamRetryableException::class.java)
    }

    @Test
    fun `503 응답 시 SteamRetryableException 발생`() {
        server.enqueue(MockResponse().setResponseCode(503))

        assertThatThrownBy {
            executor.execute({ it.path("/test").build() }, String::class.java, "testAction")
        }.isInstanceOf(SteamRetryableException::class.java)
    }

    @Test
    fun `400 응답 시 SteamApiException 발생 (재시도 대상 아님)`() {
        server.enqueue(MockResponse().setResponseCode(400).setBody("bad request"))

        assertThatThrownBy {
            executor.execute({ it.path("/test").build() }, String::class.java, "testAction")
        }.isInstanceOf(SteamApiException::class.java)
            .hasMessageContaining("testAction")
    }

    @Test
    fun `403 응답 시 SteamApiException 발생 (잘못된 API 키)`() {
        server.enqueue(MockResponse().setResponseCode(403).setBody("Forbidden"))

        assertThatThrownBy {
            executor.execute({ it.path("/test").build() }, String::class.java, "testAction")
        }.isInstanceOf(SteamApiException::class.java)
            .hasMessageContaining("403")
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

        val props = SteamProperties(baseUrl = "http://placeholder", apiKey = "test-api-key")
        val timeoutExecutor = SteamRequestExecutor(timeoutRestClient, props)

        server.enqueue(
            MockResponse()
                .setBody("{}")
                .setHeadersDelay(3, TimeUnit.SECONDS),
        )

        assertThatThrownBy {
            timeoutExecutor.execute({ it.path("/test").build() }, String::class.java, "testAction")
        }.isInstanceOf(ResourceAccessException::class.java)
    }
}
