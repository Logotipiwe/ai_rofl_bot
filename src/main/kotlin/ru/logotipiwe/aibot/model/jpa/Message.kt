package ru.logotipiwe.aibot.model.jpa

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.telegram.telegrambots.meta.api.objects.Update

@Entity
@Table(name = "updates")
//@Convert(attributeName = "update", converter = JsonBinaryType::class)
//@TypeDef(name =  "jsonb", typeClass = JsonBinaryType.class)
class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var update: Update? = null
}