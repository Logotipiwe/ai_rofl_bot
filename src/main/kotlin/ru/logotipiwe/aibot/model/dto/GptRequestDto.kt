package ru.logotipiwe.aibot.model.dto

data class GptRequestDto(
    val model: String,
    val messages: List<Message>,
){
    companion object {

    }
}

data class Message(
    val role: String,
    val content: String
)

fun gptRequestDto(block: GptRequestDtoBuilder.() -> Unit): GptRequestDto {
    return GptRequestDtoBuilder().apply(block).build()
}

class GptRequestDtoBuilder {
    var model: String = ""
    private val messages: MutableList<Message> = mutableListOf()

    fun message(block: MessageBuilder.() -> Unit) {
        messages.add(MessageBuilder().apply(block).build())
    }

    fun build(): GptRequestDto {
        return GptRequestDto(model, messages)
    }

    class MessageBuilder {
        var role: String = ""
        var content: String = ""

        fun build(): Message {
            return Message(role, content)
        }
    }
}