package com.zioanacleto.search

import com.zioanacleto.cocktails.CocktailService
import com.zioanacleto.cocktails.ExposedCocktailList
import com.zioanacleto.ingredients.IngredientsService
import com.zioanacleto.tags.ExposedTag
import com.zioanacleto.tags.TagsService
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database

class SearchService(private val database: Database) {

    suspend fun searchForCocktails(prompt: SearchRequest): ExposedCocktailList {
        return try {
            val cocktailsService = CocktailService(database)
            val ingredientsService = IngredientsService(database)
            val tagsService = TagsService(database)

            val cocktails = cocktailsService.readAll()
            val ingredients = ingredientsService.readAll()
            // val tags = tagsService.readAll()

            println("Search service, request: $prompt")
            val client = HttpClient(CIO) {
                install(ContentNegotiation) {
                    json()
                }
            }

            val url = System.getenv("AI_URL") ?: throw IllegalStateException("AI_URL not found in environment")
            val token = System.getenv("AI_TOKEN") ?: throw IllegalStateException("AI_TOKEN not found in environment")

            val rawResponse = client.post(url) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
                contentType(ContentType.Application.Json)
                setBody(
                    HuggingFaceSearchRequest(
                        inputs = prompt.query,
                        parameters = Parameters(
                            candidate_labels = ingredients.ingredients.map { it.name }
                        )
                    )
                )
                timeout {
                    requestTimeoutMillis = 30_000
                }
            }.bodyAsText()

            println("Raw AI response: $rawResponse")
            client.close()

            val response = Json.decodeFromString<SearchResponse>(rawResponse)

            println("Search api called, embeddings result: $response")

            val tagsThatMatch = response.labels.zip(response.scores)
                .filter { (_, score) -> score > SCORE_THRESHOLD }
                .map { (label, _) -> label }

            println("Tags that are over threshold: $tagsThatMatch")

            val ingredientsThatMatch = ingredients.ingredients
                .filter { ingredient -> tagsThatMatch.any { it == ingredient.name } }

            println("Ingredients contained in labels: $ingredientsThatMatch")

            val cocktailsThatMatchIngredients = cocktails.cocktails
                .filter {
                    it.ingredients.ingredients.any { cocktailIngredient ->
                        ingredientsThatMatch.any { ingredient -> ingredient.id == cocktailIngredient.id }
                    }
                }

            println("Cocktails that match ingredients: $cocktailsThatMatchIngredients")

            ExposedCocktailList(cocktailsThatMatchIngredients)
        } catch (exception: Exception) {
            println("Error while calling search API: ${exception.message}")
            exception.printStackTrace()

            throw exception
        }
    }

    companion object {
        private const val SCORE_THRESHOLD = 0.30
    }
}