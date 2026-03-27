package com.zioanacleto.cocktails

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class InstructionsTranslator(
    private val client: HttpClient
) {
    private val log = LoggerFactory.getLogger(InstructionsTranslator::class.java)

    companion object {
        private const val AI_URL_EN_IT = "TRANSLATION_AI_URL_EN_IT"
        private const val AI_URL_IT_EN = "TRANSLATION_AI_URL_IT_EN"
        private const val AI_TOKEN = "AI_TOKEN"
    }

    suspend fun translate(
        text: String,
        isFromEnglish: Boolean = true
    ): String {
        val urlConstant = if (isFromEnglish) AI_URL_EN_IT else AI_URL_IT_EN

        val url = requireNotNull(
            System.getenv(urlConstant)
        ) { "$urlConstant not found in environment" }

        val token = requireNotNull(System.getenv(AI_TOKEN)) {
            "AI_TOKEN not found in environment"
        }

        return coroutineScope {
            async {
                log.info("Translation request - direction={} textLength={}",
                    if (isFromEnglish) "EN->IT" else "IT->EN",
                    text.length
                )

                val rawResponse = client.post(url) {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $token")
                    }
                    contentType(ContentType.Application.Json)
                    setBody(
                        HuggingFaceTranslationRequest(
                            inputs = text
                        )
                    )
                    timeout {
                        requestTimeoutMillis = 30_000
                    }
                }.bodyAsText()

                log.debug("Raw AI response: $rawResponse")

                val regex = """"translation_text"\s*:\s*"([^"]+)"""".toRegex()
                regex.find(rawResponse)?.groupValues?.get(1) ?: text
            }
        }.await()
    }
}