package com.zioanacleto.search

import com.zioanacleto.tags.TagsService
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.auth.*
import io.ktor.serialization.kotlinx.json.*
import org.jetbrains.exposed.sql.Database

class SearchService(private val database: Database) {
    suspend fun queryModel(prompt: SearchRequest): String {
        val tagsService = TagsService(database)
        val tags = tagsService.readAll()

        println("Search service, request: $prompt")
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }
        }

        val response = client.post(System.getenv("AI_URL")) {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${System.getenv("AI_TOKEN")}")
            }
            contentType(ContentType.Application.Json)
            setBody(
                HuggingFaceSearchRequest(
                    inputs = prompt.query,
                    parameters = Parameters(
                        candidate_labels = tags.tags.map { it.name }
                    )
                )
            )
        }
        client.close()

        return response.body<String>()
    }
}