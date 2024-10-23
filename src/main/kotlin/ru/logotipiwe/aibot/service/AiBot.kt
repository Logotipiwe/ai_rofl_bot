package ru.logotipiwe.aibot.service

import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import ru.logotipiwe.aibot.model.Bot
import ru.logotipiwe.aibot.model.jpa.AllowedChat
import ru.logotipiwe.aibot.model.jpa.Answer
import ru.logotipiwe.aibot.model.jpa.Message
import ru.logotipiwe.aibot.repository.AllowedChatRepo
import ru.logotipiwe.aibot.repository.AnswersRepo
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
    private val allowedChatRepo: AllowedChatRepo,
    private val answersRepo: AnswersRepo,
    private val messagesService: MessagesService
) : Bot {
    private lateinit var tgClient: CustomTelegramClient

    @PostConstruct
    private fun init() {
        tgClient = CustomTelegramClient(token)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(this::class.java)
        private const val additionalPrompt = "Сначала будет запрос, потом список сообщений из чата. Если запрос не ссылается на сообщения - игнорируй их. Пиши на русском."
        private val imitationsChatToMember: MutableMap<String, String> = mutableMapOf()
    }

    override fun consume(update: Update) {
        if(update.hasMessage() && update.message.chat.id == ownerId) {
            tgClient.sendMessage(update.message.chat.id, answerOwner(update))
            return
        }
        if(update.isPersonalChat()) return replyPrivate(update)
//        if(isAllowCommand(update)) return allowChat(update)
//        if(isDenyCommand(update)) return denyChat(update)
        val savedUpdate = saveToDb(update)
//        if(!isChatAllowed(update)) return sendChatDenied(update)
        if(update.isHelpCommand) return replyHelp(update)
        val answer = answerIfCommand(update)
        if(answer != null) {
            val answerObj = Answer()
            answerObj.toUpdateId = savedUpdate.id
            answerObj.text = answer
            answersRepo.save(answerObj)
        }
    }

    private fun replyHelp(update: Update) {
        tgClient.sendMessage(update.message.chatId, """
            Я помогаю анализировать беседу. Вот как мной можно управлять:
            /summary - получить суммаризацию сообщений за сутки
            @${botLogin} - то же самое
            
            Если после тега написать вопрос, я на него отвечу. Например:
            @${botLogin} кто самый смешной в этом чате?
            Также перед вопросом можно указать цифру - кол-во часов за которые взять сообщения. Например:
            @${botLogin} 3 что тут обсуждалось?
            
            /imitate {никнейм} - попробую отвечать на ваши сообщения в манере указанного участника чата.
            Чтобы я перестал его имитировать - /stop_imitate
        """.trimIndent())
    }

    private fun answerIfCommand(update: Update): String? {
        try {
            if(testForImitateCommand(update)) return startImitation(update)
            if(testForStopImitateCommand(update)) return stopImitate(update)
            if(testForSummaryCommand(update)) return sendRoflSummary(update)

            if (testForBotPrompt(update)) return sendPromptAnswer(update)
            if (isImitating(update)) return doImitate(update)
        } catch (e: Exception){
            //реакция 🥴
            tgClient.setReaction(update, "\uD83E\uDD74")
            //сделать предварительный анализ является ли оно отсылкой к чату
            log.error(e.message, e)
            return "err"
        }
        return null
    }

    private fun doImitate(update: Update): String? {
        val chatId = update.message.chat.id.toString()
        val username = imitationsChatToMember[chatId] ?: return null
        val messages = messagesService.getMessagesOfUser(chatId, username).toMutableList()
        val firstName = messages.first().update?.message?.from?.firstName ?: return null
        val maxTokens = 3500
        val maxTokensInMessage = 100
        var messagesInStr = ""
        while(messagesInStr.length < maxTokens && messages.isNotEmpty()) {
            messagesInStr += messages.removeFirst().update?.message?.text?.take(maxTokensInMessage) ?: ""
        }
        val systemMessage = "Ты являешься $firstName, участником чата в котором он писал. Предположи кем является $firstName " +
                "и веди себя именно так. Вот его фразы\n\n$messagesInStr\n\nОтветь на сообщение адресованное $firstName. Размер твоего " +
                "ответа должен быть сопоставим с размером сообщения. Не ставь переносы строк"
        val answer = "$firstName: ${gptService.doGptRequest(systemMessage, update.message.text, 300)}"
        tgClient.sendMessage(chatId, answer)
        return answer
    }

    private fun isImitating(update: Update): Boolean {
        return imitationsChatToMember.contains(update.message.chat.id.toString())
    }

    private fun stopImitate(update: Update): String {
        val answer = "👌"
        tgClient.setReaction(update, answer)
        imitationsChatToMember.remove(update.message.chat.id.toString())
        return answer
    }

    private fun testForStopImitateCommand(update: Update): Boolean {
        return update.message.isCommand && update.message.text.startsWith("/stop_imitate")
    }

    private fun startImitation(update: Update): String {
        val text = update.message.text
        val chatId = update.message.chat.id.toString()
        if(text.split(" ").size != 2) {
            val answer = "Укажи ник кого имитировать через пробел после команды"
            tgClient.sendMessage(update.message.chat.id, answer)
            return answer
        }
        val username = text.split(" ")[1].replace("@", "")
        val usernamesInChat = messagesService.getChatMembersUsernames(chatId)
        if(usernamesInChat.contains(username).not()) {
            val answer = "Такого юзера нет, либо он совсем не писал сообщений"
            tgClient.sendMessage(chatId, answer)
            return answer
        } else {
            val answer = "Щас буду имитировать $username"
            tgClient.sendMessage(chatId, answer)
            imitationsChatToMember[chatId] = username
            return answer
        }
    }

    private fun testForImitateCommand(update: Update): Boolean {
        return update.message.isCommand && update.message.text.startsWith("/imitate")
    }

    private fun answerOwner(update: Update): String {
        val messages = messagesService.getMessagesInStr("-1002415003054", 350)
        return gptService.doGptRequest("Ты являешься Igor, участником чата в котором он писал. Вот его сообщения\n\n"
            + messages + "\n\n Ответь на сообщение. Размер твоего ответа должен быть сопоставим с размером сообщения",
            update.message.text)
    }

    private fun sendRoflSummary(update: Update): String {
        val preMessage = tgClient.sendMessage(update.message.chat.id, "Ща...")
        val ans: String?
        try {
            val messages = messagesService.getMessagesInStr(update.message.chat.id.toString(), 24)

            ans = getRoflSummary(messages)
            tgClient.sendMessage(update.message.chat.id, ans)
        } catch (e: Exception) {
            throw e
        }
        tgClient.deleteMessage(update.message.chat.id, preMessage.messageId)
        return ans
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

    private fun sendPromptAnswer(update: Update): String? {
        val preMessage = tgClient.sendMessage(update.message.chat.id, "Ща...")
        var ans: String? = null
        try {
            val words = update.message.text.split(" ")
            // убираем команду из сообщения
            val text = words.drop(1).joinToString(" ")
            val hours = if(words.size > 1) words[1].toIntOrNull() else null
            val prompt = if (hours != null) text.removePrefix(hours.toString()) else text
            val messages = messagesService.getMessagesInStr(update.message.chat.id.toString(), hours ?: 24)

            ans = if (prompt.isBlank()) getRoflSummary(messages)
                else gptService.doGptRequest(
                    additionalPrompt,
                    "$prompt\n\nЧат:\n$messages"
                )
            tgClient.sendMessage(update.message.chat.id, ans)
        } catch (e: Exception) {
            throw e
        } finally {
            tgClient.deleteMessage(update.message.chat.id, preMessage.messageId)
        }
        return ans
    }

    private fun testForBotPrompt(update: Update) =
        update.hasMessage()
                && update.message.hasText()
                && update.message.text.startsWith("@$botLogin")

    private fun testForSummaryCommand(update: Update) =
        update.hasMessage()
                && update.message.isCommand
                && update.message.text.startsWith("/summary")

    private fun isChatAllowed(update: Update): Boolean {
        return allowedChatRepo.existsById(update.message.chat.id)
    }

    private fun saveToDb(update: Update): Message {
        log.info("Received update from chat ${update.message.chat.id}")
        if (update.hasMessage() && update.message.hasText()) {
            log.info("Text is ${update.message.text}")
        }
        val message = Message()
        message.update = update
        val saved = messageRepo.saveAndFlush(message)
        log.info("Saved successfully with id ${saved.id}")
        return saved
    }

    fun getRoflSummary(messages: String): String {
        val roflPrompt =
            """Тебе дают список сообщений от разных людей, а ты кратко описываешь темы которые были обсуждены. Каждую 
                |тему опиши отдельным пунктом, кратко в одном предложении, темы раздели переносом строки. Каждую тему 
                |ты описываешь в шутливой форме, иногда подкалывая участников диалога. тебе 20 и у тебя очень 
                |подростковый зумерский сленг и юмор, иногда даже немного оскорбительный. Пиши на русском""".trimMargin()
        return gptService.doGptRequest(roflPrompt, messages)
    }

    override fun getToken(): String = token
}

private fun Update.isPersonalChat(): Boolean {
    return this.message.chat.isUserChat
}

private val Update.isHelpCommand: Boolean
    get() = hasMessage() && message.isCommand && message.text.startsWith("/help")