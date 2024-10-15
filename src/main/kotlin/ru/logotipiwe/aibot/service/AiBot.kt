package ru.logotipiwe.aibot.service

import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import ru.logotipiwe.aibot.model.Bot
import ru.logotipiwe.aibot.model.jpa.AllowedChat
import ru.logotipiwe.aibot.model.jpa.Message
import ru.logotipiwe.aibot.repository.AllowedChatRepo
import ru.logotipiwe.aibot.repository.MessageRepo
import ru.logotipiwe.aibot.service.tg.CustomTelegramClient

@Component
data class AiBot(
    @Value("\${ai-rofl-bot.token}")
    private val token: String,
    @Value("\${ai-rofl-bot.owner-id}")
    val ownerId: Long,
    @Value("\${ai-rofl-bot.botLogin}")
    val botLogin: String,
    private val messageRepo: MessageRepo,
    private val gptService: GptService,
    private val allowedChatRepo: AllowedChatRepo
) : Bot {
    private lateinit var tgClient: CustomTelegramClient

    @PostConstruct
    private fun init() {
        tgClient = CustomTelegramClient(token)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun consume(update: Update) {
        if(update.isPersonalChat()) return replyPrivate(update)
        if(isAllowCommand(update)) return allowChat(update)
        if(isDenyCommand(update)) return denyChat(update)

        saveToDb(update)

        if(!isChatAllowed(update)) return sendChatDenied(update)

        if(testForSummaryCommand(update)) return sendRoflSummary(update)
        if (testForBotPrompt(update)) sendPromptAnswer(update)
    }

    private fun sendRoflSummary(update: Update) {
        val preMessage = tgClient.sendMessage(update.message.chat.id, "Ща...")
        try {
            val messages = gptService.getMessagesInStr(update.message.chat.id.toString(), 24)

            val ans = gptService.getRoflSummary(messages)
            tgClient.sendMessage(update.message.chat.id, ans)
        } catch (e: Exception) {
            tgClient.sendMessage(update.message.chat.id, "Ошибочка")
            throw e
        }
        tgClient.deleteMessage(update.message.chat.id, preMessage.messageId)
    }

    private fun replyPrivate(update: Update) {
        tgClient.sendMessage(update.message.chat.id, "Я работаю только в групповых чатах, добавь меня туда и дай роль админа, чтобы я мог читать сообщения")
    }

    private fun sendChatDenied(update: Update) {
        tgClient.sendMessage(update.message.chat.id.toString(), "Мне не разрешили выделываться в этом чате. Попросите создателя разрешить")
    }

    private fun allowChat(update: Update) {
        val allowedChat = AllowedChat(update.message.chat.id)
        allowedChatRepo.save(allowedChat)
        tgClient.sendMessage(update.message.chat.id.toString(), "Ну поехали")
    }

    private fun denyChat(update: Update) {
        allowedChatRepo.delete(AllowedChat(update.message.chat.id))
        tgClient.sendMessage(update.message.chat.id.toString(), "Приколы закончились")
    }

    private fun isAllowCommand(update: Update): Boolean {
        return update.message.from.id == ownerId
                && update.message.text == "allow"
    }

    private fun isDenyCommand(update: Update): Boolean {
        return update.message.from.id == ownerId
                && update.message.text == "deny"
    }

    private fun sendPromptAnswer(update: Update) {
        val preMessage = tgClient.sendMessage(update.message.chat.id, "Ща...")
        try {
            val words = update.message.text.split(" ")
            // убираем команду из сообщения
            val text = words.drop(1).joinToString(" ")
            val hours = words.drop(1)[0].toIntOrNull()
            val prompt = if (hours != null) text.removePrefix(hours.toString()) else text
            val messages = gptService.getMessagesInStr(update.message.chat.id.toString(), hours ?: 24)

            val ans = if (prompt.isBlank()) gptService.getRoflSummary(messages)
                else gptService.getUserPromptAnswer(messages, prompt)
            tgClient.sendMessage(update.message.chat.id, ans)
        } catch (e: Exception) {
            tgClient.sendMessage(update.message.chat.id, "Ошибочка")
            throw e
        }
        tgClient.deleteMessage(update.message.chat.id, preMessage.messageId)
    }

    private fun testForBotPrompt(update: Update) =
        update.hasMessage()
                && update.message.hasText()
                && update.message.text.startsWith("@$botLogin")

    private fun testForSummaryCommand(update: Update) =
        update.hasMessage()
                && update.message.hasText()
                && update.message.text.startsWith("/summary")

    private fun isChatAllowed(update: Update): Boolean {
        return allowedChatRepo.existsById(update.message.chat.id)
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

private fun Update.isPersonalChat(): Boolean {
    return this.message.chat.isUserChat
}