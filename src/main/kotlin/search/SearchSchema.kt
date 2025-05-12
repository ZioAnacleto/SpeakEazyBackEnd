package com.zioanacleto.search

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.auth.*
import io.ktor.serialization.kotlinx.json.*

class SearchService {
    suspend fun queryModel(prompt: SearchRequest): String {
        println("Search service, request: $prompt, requestBody: ${prompt.toInputPrompt()}")
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }
        }

        val response = client.post(AI_URL) {
            headers {
                append(HttpHeaders.Authorization, "Bearer $AI_TOKEN")
            }
            contentType(ContentType.Application.Json)
            setBody(prompt.toInputPrompt())
        }
        client.close()

        return response.body<String>()
    }

    companion object {
        private const val AI_URL = "https://api-inference.huggingface.co/models/distilbert-base-uncased"
        private const val AI_TOKEN = "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"
    }
}