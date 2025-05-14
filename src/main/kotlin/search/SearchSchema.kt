package com.zioanacleto.search

import com.zioanacleto.cocktails.CocktailService
import com.zioanacleto.cocktails.ExposedCocktailList
import com.zioanacleto.ingredients.IngredientsService
import com.zioanacleto.tags.ExposedTag
import com.zioanacleto.tags.TagsService
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import org.jetbrains.exposed.sql.Database

class SearchService(private val database: Database) {

    suspend fun searchForCocktails(prompt: SearchRequest): ExposedCocktailList {
        val cocktailsService = CocktailService(database)
        val ingredientsService = IngredientsService(database)
        val tagsService = TagsService(database)

        val cocktails = cocktailsService.readAll()
        val ingredients = ingredientsService.readAll()
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
                        candidate_labels = tags.tags.map { it.name }.plus(ingredients.ingredients.map { it.name })
                    )
                )
            )
        }
        val searchResponse = response.body<SearchResponse>()
        client.close()

        println("Search api called, embeddings result: $searchResponse")

        val tagsThatMatch = searchResponse.labels.zip(searchResponse.scores)
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

        return ExposedCocktailList(cocktailsThatMatchIngredients)
    }

    companion object {
        private const val SCORE_THRESHOLD = 0.30
    }
}