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

class InstructionsTranslator {

    suspend fun translate(
        text: String,
        isFromEnglish: Boolean = true
    ): String {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }
        }

        val urlConstant = if (isFromEnglish) "TRANSLATION_AI_URL_EN_IT" else "TRANSLATION_AI_URL_IT_EN"

        val url = requireNotNull(
            System.getenv(urlConstant)
        ) { "$urlConstant not found in environment" }

        val token = requireNotNull(System.getenv("AI_TOKEN")) {
            "AI_TOKEN not found in environment"
        }

        return coroutineScope {
            async {
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

                println("Raw AI response: $rawResponse")
                client.close()

                val regex = """"translation_text"\s*:\s*"([^"]+)"""".toRegex()
                regex.find(rawResponse)?.groupValues?.get(1) ?: text
            }
        }.await()
    }
}