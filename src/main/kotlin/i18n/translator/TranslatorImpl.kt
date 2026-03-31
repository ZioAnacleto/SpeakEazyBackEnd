package com.zioanacleto.i18n.translator

import com.zioanacleto.admin.EnvironmentKey
import com.zioanacleto.admin.EnvironmentKeysProvider
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory

class TranslatorImpl(
    private val client: HttpClient,
    private val keysProvider: EnvironmentKeysProvider
) : Translator {

    private val log = LoggerFactory.getLogger(TranslatorImpl::class.java)

    override suspend fun translateSingleText(
        text: String,
        isFromEnglish: Boolean
    ): String {
        val environmentKey = if (isFromEnglish)
            EnvironmentKey.TRANSLATION_ENGLISH_ITALIAN
        else EnvironmentKey.TRANSLATION_ITALIAN_ENGLISH

        val url = requireNotNull(keysProvider.provideKey(environmentKey))
        val token = requireNotNull(keysProvider.provideKey(EnvironmentKey.AI_TOKEN))

        return try {
            val response = client.post(url) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
                contentType(ContentType.Application.Json)
                setBody(HuggingFaceSingleTextTranslationRequest(text))
            }.bodyAsText()

            val regex = """"translation_text"\s*:\s*"([^"]+)"""".toRegex()
            regex.find(response)?.groupValues?.get(1) ?: text

        } catch (e: Exception) {
            log.error("Single translation failed", e)
            text
        }
    }

    override suspend fun translateMultipleTexts(texts: List<String>): List<String> {
        val url = requireNotNull(
            keysProvider.provideKey(EnvironmentKey.TRANSLATION_ENGLISH_ITALIAN)
        )
        val token = requireNotNull(keysProvider.provideKey(EnvironmentKey.AI_TOKEN))

        log.info("Batch translation request size={}", texts.size)

        return try {
            val response = client.post(url) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
                contentType(ContentType.Application.Json)
                setBody(HuggingFaceMultipleTextsRequest(texts))
            }.body<List<HuggingFaceMultipleTextsResponse>>()

            response.map { it.translation_text }

        } catch (e: Exception) {
            log.error("Batch translation failed", e)
            throw e
        }
    }
}