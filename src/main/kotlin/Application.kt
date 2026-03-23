package com.zioanacleto

import com.zioanacleto.di.appModule
import com.zioanacleto.protection.configureSecurity
import com.zioanacleto.protection.startRateLimitCleanup
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
                useAlternativeNames = false
            }
        )
    }

    // Koin
    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }

    configureSecurity()
    startRateLimitCleanup()
    configureSerialization()
    configureRouting()
}
