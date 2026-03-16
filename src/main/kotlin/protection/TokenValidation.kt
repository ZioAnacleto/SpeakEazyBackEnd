package com.zioanacleto.protection

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import java.security.MessageDigest

fun Application.configureTokenValidation() {
    intercept(ApplicationCallPipeline.Plugins) {
        val receivedToken = call.request.headers[HEADER_AUTHORIZATION]
            ?.removePrefix("Bearer ")
            ?.trim()
        val expectedToken = System.getenv(ENVIRONMENT_TOKEN).hashToken()

        println()

        if (receivedToken == null || !expectedToken.equals(receivedToken)) {
            call.respondText("Unauthorized", status = HttpStatusCode.Unauthorized)
            finish()
        }
    }
}

private fun String.hashToken(): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(this.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

const val HEADER_AUTHORIZATION = "Authorization"
const val ENVIRONMENT_TOKEN = "API_TOKEN"