package ru.logotipiwe.aibot.model.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class GptRequestDto(
    val model: String,
    @JsonProperty("max_tokens")
    val maxTokens: Int?,
    val messages: List<Message>,
)

data class Message(
    val role: String,
    val content: String
)

fun gptRequestDto(block: GptRequestDtoBuilder.() -> Unit): GptRequestDto {
    return GptRequestDtoBuilder().apply(block).build()
}

class GptRequestDtoBuilder {
    var model: String = ""
    var maxTokens: Int? = null
    private val messages: MutableList<Message> = mutableListOf()

    fun message(block: MessageBuilder.() -> Unit) {
        messages.add(MessageBuilder().apply(block).build())
    }

    fun build(): GptRequestDto {
        return GptRequestDto(model, maxTokens, messages)
    }

    class MessageBuilder {
        var role: String = ""
        var content: String = ""

        fun build(): Message {
            return Message(role, content)
        }
    }
}