package ru.logotipiwe.aibot.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.logotipiwe.aibot.model.jpa.Message

interface MessageRepo: JpaRepository<Message, Long>