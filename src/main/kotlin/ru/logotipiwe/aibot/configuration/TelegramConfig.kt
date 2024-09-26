package ru.logotipiwe.aibot.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication
import ru.logotipiwe.aibot.model.Bot

@Configuration
class TelegramConfig {
    @Bean
    fun aiRoflBot(
        bots: List<Bot>
    ): TelegramBotsLongPollingApplication {
        val app = TelegramBotsLongPollingApplication()
        bots.forEach { app.registerBot(it) }

        return app
    }

    fun TelegramBotsLongPollingApplication.registerBot(bot: Bot) {
        this.registerBot(bot.getToken(), bot)
    }
}