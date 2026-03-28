package com.zioanacleto.cocktails

import com.zioanacleto.admin.EnvironmentKey
import com.zioanacleto.admin.EnvironmentKeysProvider
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory

class InstructionsTranslator(
    private val client: HttpClient,
    private val keysProvider: EnvironmentKeysProvider
) {
    private val log = LoggerFactory.getLogger(InstructionsTranslator::class.java)

    suspend fun translate(
        text: String,
        isFromEnglish: Boolean = true
    ): String {
        val environmentKey = if (isFromEnglish)
            EnvironmentKey.TRANSLATION_ENGLISH_ITALIAN
        else EnvironmentKey.TRANSLATION_ITALIAN_ENGLISH

        val url = requireNotNull(
            keysProvider.provideKey(environmentKey)
        ) { "${environmentKey.key} not found in environment" }

        val token = requireNotNull(keysProvider.provideKey(EnvironmentKey.AI_TOKEN)) {
            "${EnvironmentKey.AI_TOKEN.key} not found in environment"
        }

        return coroutineScope {
            async {
                log.info(
                    "Translation request - direction={} textLength={}",
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