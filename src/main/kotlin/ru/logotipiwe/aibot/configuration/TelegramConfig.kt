package ru.logotipiwe.aibot.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication
import ru.logotipiwe.aibot.model.Bot
import ru.logotipiwe.aibot.service.AiBot

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

    @Bean
    fun botsRegistry(
        aiBot: AiBot
    ): BotsRegistry {
        return BotsRegistry(aiBot)
    }

    fun TelegramBotsLongPollingApplication.registerBot(bot: Bot) {
        this.registerBot(bot.getToken(), bot)
    }
}