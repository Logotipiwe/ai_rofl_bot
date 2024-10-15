package ru.logotipiwe.aibot.model.jpa

import jakarta.persistence.*

@Entity
@Table(name = "answers")
open class Answer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "to_update")
    var toUpdateId: Long? = null

    var text: String? = null
}
