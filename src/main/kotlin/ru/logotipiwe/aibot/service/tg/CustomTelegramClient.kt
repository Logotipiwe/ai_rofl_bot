package ru.logotipiwe.aibot.service.tg

import org.slf4j.LoggerFactory
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.reactions.SetMessageReaction
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.api.objects.reactions.ReactionType
import org.telegram.telegrambots.meta.api.objects.reactions.ReactionTypeEmoji
import org.telegram.telegrambots.meta.generics.TelegramClient

class CustomTelegramClient(token: String) {
    val tgClient: TelegramClient = OkHttpTelegramClient(token)

    companion object {
        private val logger = LoggerFactory.getLogger(CustomTelegramClient::class.java)
        private val MAX_MESSAGE_SIZE = 4096
    }

    fun sendMessage(chatId: Long, message: String,
                    replyTo: Int? = null): Message {
        return sendMessage(chatId.toString(), message, replyTo)
    }

    fun sendMessage(chatId: String, message: String, replyTo: Int? = null): Message {
        var result: Message? = null
        if(message.isBlank()) throw IllegalArgumentException("message should not be blank")
        message.chunked(MAX_MESSAGE_SIZE).forEach {
            val sendMessage = SendMessage(chatId, it)
            sendMessage.parseMode = ParseMode.MARKDOWN
            if(replyTo != null) {
                sendMessage.replyToMessageId = replyTo
            }
            val ans = tgClient.execute(sendMessage)
            if(result == null) result = ans
        }
        return result!!
    }

    fun deleteMessage(chatId: Long, messageId: Int): Boolean {
        return tgClient.execute(DeleteMessage(chatId.toString(), messageId))
    }

    fun setReaction(update: Update, emoji: String): Boolean {
        val reactionTypes: MutableList<ReactionType> = mutableListOf(ReactionTypeEmoji(ReactionType.EMOJI_TYPE, emoji))
        val setMessageReaction = SetMessageReaction(update.message.chat.id.toString(), update.message.messageId,
            reactionTypes, false
        )
        return tgClient.execute(setMessageReaction)
    }
}