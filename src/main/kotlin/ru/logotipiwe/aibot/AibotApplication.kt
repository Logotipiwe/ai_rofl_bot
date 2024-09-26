package ru.logotipiwe.aibot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan("ru.logotipiwe")
class AibotApplication

fun main(args: Array<String>) {
    runApplication<AibotApplication>(*args)
}
