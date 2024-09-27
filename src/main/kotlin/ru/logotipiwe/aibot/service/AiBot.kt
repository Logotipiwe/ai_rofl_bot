package ru.logotipiwe.aibot.service

import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.logotipiwe.aibot.model.Bot
import ru.logotipiwe.aibot.model.jpa.Message
import ru.logotipiwe.aibot.repository.MessageRepo

@Component
data class AiBot(
    @Value("\${ai-rofl-bot.token}")
    private val token: String,
    @Value("\${ai-rofl-bot.owner-id}")
    val ownerId: Long,
    private val messageRepo: MessageRepo,
    val gptService: GptService,
): Bot {
    private lateinit var tgClient: TelegramClient

    @PostConstruct
    private fun init() {
        tgClient = OkHttpTelegramClient(token)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun consume(update: Update) {
        if(update.hasMessage() && update.message.hasText()) {
            if(update.message.from.id == ownerId && update.message.text == "давай саммари") {
                tgClient.execute(SendMessage(update.message.chat.id.toString(), "Ща будет саммари"))
                val messages = gptService.getMessagesInStr(update.message.chat.id.toString())
                log.info("messages: $messages")
                val answer = gptService.getGptAnswer(messages)
                tgClient.execute(SendMessage(update.message.chat.id.toString(), answer))
            }
        } else {
            saveToDb(update)
        }
    }

    private fun saveToDb(update: Update) {
        log.info("Received update from chat ${update.message.chat.id}")
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