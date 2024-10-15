package ru.logotipiwe.aibot.service

import org.springframework.stereotype.Service
import ru.logotipiwe.aibot.model.jpa.Message
import ru.logotipiwe.aibot.repository.MessageRepo

@Service
data class MessagesService(
    private var messageRepo: MessageRepo
) {
    fun getMessagesInStr(chatId: String, hours: Int): String {
        val messages: List<Message> = messageRepo.getByChatIdForHours(chatId, hours)
        return messages.joinToString("\n") { m ->
            (m.update?.message?.from?.firstName + ": " + m.update?.message?.text)
        }
    }

    fun getChatMembersUsernames(chatId: String): Set<String> {
        return messageRepo.getChatMembersUsernames(chatId)
    }

    fun getMessagesOfUser(chatId: String, userName: String): List<Message> {
        return messageRepo.getMessagesByUserName(chatId, userName)
    }
}