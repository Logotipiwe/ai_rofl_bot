package ru.logotipiwe.aibot.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.logotipiwe.aibot.model.jpa.Answer

@Repository
interface AnswersRepo: JpaRepository<Answer, Long>