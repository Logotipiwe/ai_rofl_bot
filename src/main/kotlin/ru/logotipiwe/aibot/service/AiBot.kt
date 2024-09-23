package ru.logotipiwe.aibot.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import ru.logotipiwe.aibot.model.Bot
import ru.logotipiwe.aibot.model.jpa.Message
import ru.logotipiwe.aibot.repository.MessageRepo
import ru.logotipiwe.aibot.service.tg.client.AiBotClient

@Component
data class AiBot(
    private val aiBotClient: AiBotClient,
    @Value("\${ai-rofl-bot.token}")
    private val token: String,
    private val messageRepo: MessageRepo,
): Bot {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun consume(update: Update) {
        log.info("Received update from chat ${update.message.chatId}")
        if (update.hasMessage() && update.message.hasText()) {
            log.info("Text is ${update.message.text}")
        }
        val message = Message()
        message.update = update
        try {
            val saved = messageRepo.save(message)
            log.info("Saved successfully with id ${saved.id}")
        } catch (e: Exception) {
            log.error("Error saving update", e)
        }

    }

    override fun getToken(): String = token
}