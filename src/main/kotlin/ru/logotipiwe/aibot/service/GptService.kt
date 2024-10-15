package ru.logotipiwe.aibot.service

import org.apache.logging.log4j.LogManager.getLogger
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import ru.logotipiwe.aibot.configuration.GptConfig
import ru.logotipiwe.aibot.model.dto.GptResponseDto
import ru.logotipiwe.aibot.model.dto.gptRequestDto
import ru.logotipiwe.aibot.repository.MessageRepo

@Service
data class GptService(
    val restTemplate: RestTemplate,
    val gptConfig: GptConfig,
    val messageRepo: MessageRepo
) {
    companion object {
        private val log = getLogger(GptService::class.java)
    }

    fun doGptRequest(systemMessage: String, userMessage: String, maxTokens: Int? = null): String {
        val headers = HttpHeaders()
        headers.set("Content-Type", "application/json")
        headers.set("Authorization", "Bearer ${gptConfig.token}")

        val request = gptRequestDto {
            model = "google/gemma-2-9b-it"
            this.maxTokens = maxTokens
            message {
                role = "system"
                content = systemMessage
            }
            message {
                role = "user"
                content = userMessage
            }
        }
        val entity = HttpEntity(request, headers)
        val url = "https://api.vsegpt.ru/v1/chat/completions"

        log.info("Sending request to gpt...")
        log.info("Input tokens: ${(systemMessage+userMessage).length}")
        val response = restTemplate.exchange(url, HttpMethod.POST, entity, GptResponseDto::class.java)
        if(response.statusCode.is2xxSuccessful.not()) {
            throw RuntimeException(response.body.toString())
        }
        val answer = response.body!!.choices.joinToString(". ") { c -> c.message?.content ?: "net kontenta" }
        log.info("Got answer from gpt.")
        log.info("Output tokens: ${answer.length}")
        return answer
    }
}