package ru.logotipiwe.aibot.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate

@Configuration
class RestTemplateConfig {
    @Bean
    fun restTemplate(): RestTemplate {
        val requestFactory = HttpComponentsClientHttpRequestFactory().apply {  }
        val restTemplate = RestTemplate(requestFactory)
        restTemplate.interceptors = listOf(CustomHeaderRemovingInterceptor())
        return restTemplate
    }
}

class CustomHeaderRemovingInterceptor : ClientHttpRequestInterceptor {
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {
        // Удаляем все заголовки перед выполнением запроса
        request.headers.clear()
        // Добавьте свои заголовки, если нужно
        return execution.execute(request, body)
    }
}