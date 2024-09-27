package ru.logotipiwe.aibot.service

import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import ru.logotipiwe.aibot.configuration.GptConfig
import ru.logotipiwe.aibot.model.dto.GptResponseDto
import ru.logotipiwe.aibot.model.dto.gptRequestDto
import ru.logotipiwe.aibot.model.jpa.Message
import ru.logotipiwe.aibot.repository.MessageRepo

@Service
data class GptService(
    val restTemplate: RestTemplate,
    val gptConfig: GptConfig,
    val messageRepo: MessageRepo
) {
    fun getMessagesInStr(chatId: String): String {
        val messages: List<Message> = messageRepo.getByChatIdForLastDay(chatId)
        return messages.joinToString("\n") { m ->
            (m.update?.message?.from?.firstName + ": " + m.update?.message?.text)
        }
    }

    fun getGptAnswer(messages: String): String {
        val headers = HttpHeaders()
        headers.set("Content-Type", "application/json")
        headers.set("Authorization", "Bearer ${gptConfig.token}")

        val request = gptRequestDto {
            model = "openai/gpt-4o-mini"
            message {
                role = "system"
                content = "Тебе дают список сообщений от разных людей, а ты описываешь темы которые были обсуждены. Каждую тему опиши отдельным пунктом, в 1-2 предложениях, темы раздели переносом строки. Каждую тему ты описываешь в шутливой форме,  иногда подкалывая участников диалога. тебе 20 и у тебя очень подростковый зумерский сленг и юмор, иногда даже немного скорбительный. Ты пишешь по русски"
            }
            message {
                role = "user"
                content = messages
            }
        }

        val entity = HttpEntity(request, headers)
        val url = "https://api.vsegpt.ru/v1/chat/completions"

        val response = restTemplate.exchange(url, HttpMethod.POST, entity, GptResponseDto::class.java)
        if(response.statusCode.is2xxSuccessful.not()) {
            throw RuntimeException(response.body.toString())
        }
        return response.body!!.choices.joinToString(". ") { c -> c.message.content }
    }
}