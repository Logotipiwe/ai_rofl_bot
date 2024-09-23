package ru.logotipiwe.aibot.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.logotipiwe.aibot.model.Bot

@Configuration
class TelegramConfig {
    @Bean
    fun tgClient(): TelegramClient {
        return OkHttpTelegramClient("7811476690:AAGf7JD72Hc2vRQXqGkL3NxdDWskqhtTFzU")
    }

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