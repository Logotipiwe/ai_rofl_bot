package ru.logotipiwe.aibot.service.tg

import org.slf4j.LoggerFactory
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.generics.TelegramClient

class CustomTelegramClient(token: String) {
    val tgClient: TelegramClient = OkHttpTelegramClient(token)

    companion object {
        private val logger = LoggerFactory.getLogger(CustomTelegramClient::class.java)
        private val MAX_MESSAGE_SIZE = 4096
    }

    fun sendMessage(chatId: Long, message: String): Message {
        return sendMessage(chatId.toString(), message)
    }

    fun sendMessage(chatId: String, message: String): Message {
        var result: Message? = null
        if(message.isBlank()) throw IllegalArgumentException("message should not be blank")
        message.chunked(MAX_MESSAGE_SIZE).forEach {
            val ans = tgClient.execute(SendMessage(chatId, it))
            if(result == null) result = ans
        }
        return result!!
    }

    fun deleteMessage(chatId: Long, messageId: Int): Boolean {
        return tgClient.execute(DeleteMessage(chatId.toString(), messageId))
    }
}