package com.zioanacleto.protection

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*

const val API_KEY_HEADER = "X-API-Key"

val validApiKeys = System.getenv("API_KEYS")
    ?.split(",")
    ?.toSet()
    ?: emptySet()

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

        val ip = call.request.origin.remoteHost
        val apiKey = call.request.headers[API_KEY_HEADER]

        // validate api key
        if (apiKey == null || !validApiKeys.contains(apiKey)) {
            call.respond(HttpStatusCode.Unauthorized, "Invalid API Key")
            finish()
            return@intercept
        }

        val (ipAllowed, ipRemaining) = ipLimiter.isAllowed(ip)

        // ip check to avoid too many requests
        if (!ipAllowed) {
            call.respond(HttpStatusCode.TooManyRequests, "Too many requests from IP")
            finish()
            return@intercept
        }

        val (keyAllowed, keyRemaining) = apiKeyLimiter.isAllowed(apiKey)

        // api key rate limiter
        if (!keyAllowed) {
            call.respond(HttpStatusCode.TooManyRequests, "API key rate limit exceeded")
            finish()
            return@intercept
        }

        call.response.headers.append("X-RateLimit-IP-Remaining", ipRemaining.toString())
        call.response.headers.append("X-RateLimit-Key-Remaining", keyRemaining.toString())
    }
}