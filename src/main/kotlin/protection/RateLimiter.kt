package com.zioanacleto.protection

import io.ktor.server.application.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.fixedRateTimer

data class RateLimitEntry(
    var count: Int,
    var windowStart: Long
)

class RateLimiter(
    private val maxRequests: Int,
    private val windowMs: Long
) {
    private val storage = ConcurrentHashMap<String, RateLimitEntry>()

    fun isAllowed(key: String): Pair<Boolean, Int> {
        val now = System.currentTimeMillis()

        val entry = storage.compute(key) { _, current ->
            if (current == null) {
                RateLimitEntry(1, now)
            } else {
                if (now - current.windowStart > windowMs) {
                    current.count = 1
                    current.windowStart = now
                } else {
                    current.count++
                }

                current
            }
        }!!

        val remaining = maxRequests - entry.count

        return (entry.count <= maxRequests) to remaining
    }

    fun cleanup() {
        val now = System.currentTimeMillis()
        storage.entries.removeIf { now - it.value.windowStart > windowMs }
    }
}

fun Application.startRateLimitCleanup() {
    fixedRateTimer(
        name = "rate-limit-cleaner",
        daemon = true,
        initialDelay = 60_000,
        period = 60_000
    ) {
        ipLimiter.cleanup()
        apiKeyLimiter.cleanup()
    }
}