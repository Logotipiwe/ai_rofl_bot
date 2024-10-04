package ru.logotipiwe.aibot.model.jpa

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import lombok.AllArgsConstructor
import lombok.Data

@Data
@Entity
@Table(name = "allowed_chats")
@AllArgsConstructor
class AllowedChat(@Id private val id: Long? = null) {
}
