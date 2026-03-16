package com.zioanacleto

import com.zioanacleto.protection.configureSecurity
import com.zioanacleto.protection.configureTokenValidation
import com.zioanacleto.protection.startRateLimitCleanup
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json

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

    // configureTokenValidation()
    configureSecurity()
    startRateLimitCleanup()
    configureSerialization()
    configureRouting(configureDatabase())
}
