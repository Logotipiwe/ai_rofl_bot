package ru.logotipiwe.aibot.model.dto

data class GptResponseDto(
    val id: String?,
    val model: String?,
    val `object`: String?,
    val created: Long,
    val choices: List<Choice>,
    val systemFingerprint: String?,
    val usage: Usage?
) {
    data class Choice(
        val logprobs: Any?,
        val finishReason: String?,
        val index: Int?,
        val message: Message?
    ) {
        data class Message(
            val role: String?,
            val content: String,
            val refusal: String?
        )
    }

    data class Usage(
        val promptTokens: Int?,
        val completionTokens: Int?,
        val totalTokens: Int?
    )
}
