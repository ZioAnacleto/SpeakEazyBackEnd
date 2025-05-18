package com.zioanacleto.search

import com.zioanacleto.cocktails.CocktailService
import com.zioanacleto.cocktails.ExposedCocktailList
import com.zioanacleto.ingredients.IngredientsService
import com.zioanacleto.tags.TagsService
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database

class SearchService(private val database: Database) {

    suspend fun searchForCocktails(prompt: SearchRequest): ExposedCocktailList {
        return try {
            val cocktailsService = CocktailService(database)
            val ingredientsService = IngredientsService(database)
            val tagsService = TagsService(database)

            // retrieve data from DB (cocktails, ingredients, tags)
            val cocktails = asyncCall { cocktailsService.readAll() }
            val ingredients = asyncCall { ingredientsService.readAll() }
            val tags = asyncCall { tagsService.readAll() }

            println("Search service, request: $prompt")

            // perform two different api calls in parallel
            val tagsResponse = performHuggingFaceApiCall(
                prompt.query,
                tags.tags.map { it.name }
            )

            println("Search api called, tagsResponse: $tagsResponse")

            val ingredientsResponse = performHuggingFaceApiCall(
                prompt.query,
                ingredients.ingredients.map { it.name }
            )

            println("Search api called, ingredientsResponse: $ingredientsResponse")

            // select tags and ingredients over threshold, that match the prompt the most
            val tagsOverThreshold = tagsResponse.computeAcceptableLabels()
            val ingredientsOverThreshold = ingredientsResponse.computeAcceptableLabels()

            println("Tags that are over threshold: $tagsOverThreshold")
            println("Ingredients that are over threshold: $ingredientsOverThreshold")

            // find ingredients that match
            val ingredientsThatMatch = ingredients.ingredients
                .filter { ingredient -> ingredientsOverThreshold.any { it == ingredient.name } }

            println("Ingredients contained in labels: $ingredientsThatMatch")

            // find cocktail using those ingredients
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

    private suspend fun performHuggingFaceApiCall(
        prompt: String,
        candidateLabels: List<String>
    ): SearchResponse {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }
        }

        val url = requireNotNull(System.getenv("AI_URL")) {
            "AI_URL not found in environment"
        }
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
                        HuggingFaceSearchRequest(
                            inputs = prompt,
                            parameters = Parameters(
                                candidate_labels = candidateLabels
                            )
                        )
                    )
                    timeout {
                        requestTimeoutMillis = 30_000
                    }
                }.bodyAsText()

                println("Raw AI response: $rawResponse")
                client.close()

                Json.decodeFromString<SearchResponse>(rawResponse)
            }
        }.await()
    }

    private fun SearchResponse.computeAcceptableLabels() =
        labels.zip(this.scores)
            .filter { (_, score) -> score > SCORE_THRESHOLD }
            .map { (label, _) -> label }

    private suspend fun <Response> asyncCall(block: suspend CoroutineScope.() -> Response) =
        coroutineScope { async { block() }.await() }

    companion object {
        private const val SCORE_THRESHOLD = 0.30
    }
}