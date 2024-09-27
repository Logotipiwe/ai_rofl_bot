package ru.logotipiwe.aibot.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.logotipiwe.aibot.model.jpa.Message

interface MessageRepo: JpaRepository<Message, Long>{
    @Query(nativeQuery = true, value = """SELECT *
        FROM "ai-rofl-bot".updates u
        WHERE u.update->'message'->'chat'->>'id' = :chatId
        AND u.update->'message'->>'text' is not null
        AND to_timestamp(cast(u.update->'message'->>'date' as bigint)) > current_timestamp - interval '1 day'
        ORDER BY id asc"""
    )
    fun getByChatIdForLastDay(@Param("chatId") chatId: String): List<Message>
}