package com.zioanacleto.search

import com.zioanacleto.asyncCall
import com.zioanacleto.cocktails.ExposedCocktailList
import com.zioanacleto.cocktails.service.CocktailsService
import com.zioanacleto.ingredients.service.IngredientsService
import com.zioanacleto.tags.service.TagsService
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class SearchService(
    private val cocktailsService: CocktailsService,
    private val ingredientsService: IngredientsService,
    private val tagsService: TagsService,
    private val httpClient: HttpClient
) {
    private val log = LoggerFactory.getLogger(SearchService::class.java)

    suspend fun searchForCocktails(query: String): ExposedCocktailList {
        return try {
            // retrieve data from DB (cocktails, ingredients, tags)
            val cocktails = asyncCall { cocktailsService.readAll() }

            log.debug("SearchForCocktails, request: {}", query)

            val matchingCocktails = cocktails.cocktails.filter { cocktail ->
                val matchName = cocktail.name.contains(query, ignoreCase = true)
                val matchIngredient =
                    cocktail.ingredients.ingredients.any { ing ->
                        ing.name.contains(query, ignoreCase = true)
                    }

                matchName || matchIngredient
            }

            ExposedCocktailList(matchingCocktails)
        } catch (exception: Exception) {
            log.debug("Error while calling searchForCocktails API: ${exception.message}")
            exception.printStackTrace()

            throw exception
        }
    }

    suspend fun searchForCocktailsUsingHuggingFace(prompt: SearchRequest): ExposedCocktailList {
        return try {
            // retrieve data from DB (cocktails, ingredients, tags)
            val cocktails = asyncCall { cocktailsService.readAll() }
            val ingredients = asyncCall { ingredientsService.readAll() }
            val tags = asyncCall { tagsService.readAll() }

            log.debug("searchForCocktailsUsingHuggingFace, request: {}", prompt)

            // perform two different api calls in parallel
            val tagsResponse = asyncCall {
                performHuggingFaceApiCall(
                    prompt.query,
                    tags.tags.map { it.name }
                )
            }
            val ingredientsResponse = asyncCall {
                performHuggingFaceApiCall(
                    prompt.query,
                    ingredients.ingredients.map { it.name }
                )
            }

            log.debug("Search api called, tagsResponse: {}", tagsResponse)
            log.debug("Search api called, ingredientsResponse: {}", ingredientsResponse)

            // select tags and ingredients over threshold, that match the prompt the most
            val tagsOverThreshold = tagsResponse.computeAcceptableLabels()
            val ingredientsOverThreshold = ingredientsResponse.computeAcceptableLabels()

            log.debug("Tags that are over threshold: {}", tagsOverThreshold)
            log.debug("Ingredients that are over threshold: {}", ingredientsOverThreshold)

            // find ingredients that match
            val ingredientsThatMatch = ingredients.ingredients
                .filter { ingredient -> ingredientsOverThreshold.any { it == ingredient.name } }

            log.debug("Ingredients contained in labels: {}", ingredientsThatMatch)

            // find cocktail using those ingredients
            val cocktailsThatMatchIngredients = cocktails.cocktails
                .filter {
                    it.ingredients.ingredients.any { cocktailIngredient ->
                        ingredientsThatMatch.any { ingredient -> ingredient.id == cocktailIngredient.id }
                    }
                }

            log.debug("Cocktails that match ingredients: {}", cocktailsThatMatchIngredients)

            ExposedCocktailList(cocktailsThatMatchIngredients)
        } catch (exception: Exception) {
            log.debug("Error while calling searchForCocktailsUsingHuggingFace API: ${exception.message}")
            exception.printStackTrace()

            throw exception
        }
    }

    // To be updated to a more scalable version
    suspend fun filterCocktails(
        nameQuery: String?,
        ingredientsQuery: List<String> = emptyList(),
        tagsQuery: List<String> = emptyList()
    ): ExposedCocktailList {
        // reading from DB
        val allCocktails = asyncCall { cocktailsService.readAll() }
        val allTags = asyncCall { tagsService.readAll() }

        val filtered = allCocktails.cocktails.filter { cocktail ->
            val matchName = nameQuery?.let { cocktail.name.contains(it, ignoreCase = true) } ?: true
            val matchIngredient = ingredientsQuery.isEmpty() || ingredientsQuery.any { query ->
                cocktail.ingredients.ingredients.any { ing -> ing.name.equals(query, ignoreCase = true) }
            }
            val matchTag = tagsQuery.isEmpty() || tagsQuery.any {
                cocktail.tags.tags.any { tagId ->
                    allTags.tags.any { it.id == tagId.id }
                }
            }

            matchName && matchIngredient && matchTag
        }

        return ExposedCocktailList(filtered)
    }

    private suspend fun performHuggingFaceApiCall(
        prompt: String,
        candidateLabels: List<String>
    ): SearchResponse {
        val url = requireNotNull(System.getenv("AI_URL")) {
            "AI_URL not found in environment"
        }
        val token = requireNotNull(System.getenv("AI_TOKEN")) {
            "AI_TOKEN not found in environment"
        }

        val rawResponse = httpClient.post(url) {
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

        log.debug("Raw AI response: $rawResponse")

        return Json.decodeFromString<SearchResponse>(rawResponse)
    }

    private fun SearchResponse.computeAcceptableLabels() =
        labels.zip(this.scores)
            .filter { (_, score) -> score > SCORE_THRESHOLD }
            .map { (label, _) -> label }

    companion object {
        private const val SCORE_THRESHOLD = 0.30
    }
}