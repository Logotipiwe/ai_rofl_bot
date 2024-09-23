package ru.logotipiwe.aibot.model

import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer

interface Bot: LongPollingSingleThreadUpdateConsumer {
    fun getToken(): String
}