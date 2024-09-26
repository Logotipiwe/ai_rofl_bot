package ru.logotipiwe.aibot.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "gpt")
data class GptConfig(
    val token: String
)