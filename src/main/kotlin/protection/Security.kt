package com.zioanacleto.protection

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*

const val API_KEY_HEADER = "X-API-Key"

val validApiKeys = System.getenv("API_KEYS")
    ?.split(",")
    ?.associate {
        val (name, key) = it.split(":")
        key to name
    } ?: emptyMap()

val ipLimiter = RateLimiter(
    maxRequests = 120,
    windowMs = 60_000
)

val apiKeyLimiter = RateLimiter(
    maxRequests = 1000,
    windowMs = 60 * 60 * 1000
)

fun Application.configureSecurity() {
    intercept(ApplicationCallPipeline.Plugins) {
        val log = application.log
        val ip = call.request.origin.remoteHost
        val path = call.request.path()
        val method = call.request.httpMethod.value

        val apiKey = call.request.headers[API_KEY_HEADER]
        val apiKeyName = apiKey?.let { validApiKeys[it] } ?: "UNKNOWN"

        if (apiKey == null || !validApiKeys.contains(apiKey)) {

            log.warn(
                "Unauthorized request - IP=$ip PATH=$path METHOD=$method API_KEY=$apiKeyName"
            )

            call.respond(HttpStatusCode.Unauthorized, "Invalid API Key")
            finish()
            return@intercept
        }

        val (ipAllowed, ipRemaining) = ipLimiter.isAllowed(ip)

        if (!ipAllowed) {

            log.warn(
                "IP rate limit exceeded - IP=$ip PATH=$path METHOD=$method API_KEY=$apiKeyName"
            )

            call.respond(HttpStatusCode.TooManyRequests, "Too many requests from IP")
            finish()
            return@intercept
        }

        val (keyAllowed, keyRemaining) = apiKeyLimiter.isAllowed(apiKey)

        if (!keyAllowed) {

            log.warn(
                "API key rate limit exceeded - API_KEY=$apiKeyName IP=$ip PATH=$path METHOD=$method"
            )

            call.respond(HttpStatusCode.TooManyRequests, "API key rate limit exceeded")
            finish()
            return@intercept
        }

        log.info("SecurityInfo - API_KEY=$apiKeyName IP=$ip PATH=$path METHOD=$method")
        call.response.headers.append("X-RateLimit-IP-Remaining", ipRemaining.toString())
        call.response.headers.append("X-RateLimit-Key-Remaining", keyRemaining.toString())
    }
}