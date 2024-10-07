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
    companion object {
        private val additionalPrompt = "Сначала будет запрос, потом список сообщений из чата. Если запрос не ссылается на сообщения - игнорируй их. Пиши на русском."
    }
    fun getMessagesInStr(chatId: String, hours: Int): String {
        val messages: List<Message> = messageRepo.getByChatIdForHours(chatId, hours)
        return messages.joinToString("\n") { m ->
            (m.update?.message?.from?.firstName + ": " + m.update?.message?.text)
        }
    }

    fun getGptAnswer(messages: String, prompt: String?): String {
        val headers = HttpHeaders()
        headers.set("Content-Type", "application/json")
        headers.set("Authorization", "Bearer ${gptConfig.token}")

        val defaultPrompt =
            """Тебе дают список сообщений от разных людей, а ты кратко описываешь темы которые были обсуждены. Каждую 
                |тему опиши отдельным пунктом, кратко в одном предложении, темы раздели переносом строки. Каждую тему 
                |ты описываешь в шутливой форме, иногда подкалывая участников диалога. тебе 20 и у тебя очень 
                |подростковый зумерский сленг и юмор, иногда даже немного оскорбительный. Пиши на русском""".trimMargin()
        val request = gptRequestDto {
            model = "google/gemma-2-9b-it"
            message {
                role = "system"
                content = if(prompt.isNullOrEmpty()) defaultPrompt else additionalPrompt
            }
            message {
                role = "user"
                content = if (prompt.isNullOrEmpty())
                    messages
                    else prompt + "\n\nЧат:\n" + messages
            }
        }

        val entity = HttpEntity(request, headers)
        val url = "https://api.vsegpt.ru/v1/chat/completions"

        val response = restTemplate.exchange(url, HttpMethod.POST, entity, GptResponseDto::class.java)
        if(response.statusCode.is2xxSuccessful.not()) {
            throw RuntimeException(response.body.toString())
        }
        return response.body!!.choices.joinToString(". ") { c -> c.message?.content ?: "net kontenta" }
    }
}