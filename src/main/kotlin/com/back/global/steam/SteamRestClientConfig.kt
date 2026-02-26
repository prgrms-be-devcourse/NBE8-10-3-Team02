package com.back.global.steam

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

@Configuration
class SteamRestClientConfig {
    companion object {
        private val CONNECT_TIMEOUT = Duration.ofSeconds(5)
        private val READ_TIMEOUT = Duration.ofSeconds(10)
    }

    @Bean
    fun steamRestClient(
        builder: RestClient.Builder,
        props: SteamProperties,
    ): RestClient {
        val httpClient =
            HttpClient
                .newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build()
        val requestFactory = JdkClientHttpRequestFactory(httpClient)
        requestFactory.setReadTimeout(READ_TIMEOUT)
        return builder
            .baseUrl(props.baseUrl)
            .requestFactory(requestFactory)
            .build()
    }
}
