package com.zioanacleto.search

import com.zioanacleto.asyncCall
import com.zioanacleto.baseGetApi
import com.zioanacleto.basePostApi
import com.zioanacleto.cocktails.CocktailService
import com.zioanacleto.cocktails.ExposedCocktailList
import com.zioanacleto.tags.TagsService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database

fun Routing.setupSearchRouting(database: Database) {
    val searchService = SearchService(database)

    post("/search") {
        val request = call.receive<SearchRequest>()
        basePostApi(request) {
            val cocktails = searchService.searchForCocktails(request)
            call.respond(HttpStatusCode.OK, cocktails)

            cocktails
        }
    }

    get("/search/filter") {
        baseGetApi {
            val nameQuery = call.request.queryParameters["name"]
            val ingredientQuery = call.request.queryParameters.getAll("ingredient") ?: emptyList()
            val tagQuery = call.request.queryParameters.getAll("tag") ?: emptyList()

            println("nameQuery: $nameQuery")
            println("ingredientQuery: $ingredientQuery")
            println("tagQuery: $tagQuery")

            // reading from DB
            val allCocktails = asyncCall { CocktailService(database).readAll() }
            val allTags = asyncCall { TagsService(database).readAll() }

            val filtered = allCocktails.cocktails.filter { cocktail ->
                val matchName = nameQuery?.let { cocktail.name.contains(it, ignoreCase = true) } ?: true
                val matchIngredient = ingredientQuery.isEmpty() || ingredientQuery.any {
                    cocktail.ingredients.ingredients.any { ing -> ing.name.equals(it, ignoreCase = true) }
                }
                val matchTag = tagQuery.isEmpty() || tagQuery.any {
                    cocktail.tags.tags.any { tagId ->
                        allTags.tags.any { it.id == tagId.id }
                    }
                }

                matchName && matchIngredient && matchTag
            }

            call.respond(HttpStatusCode.OK, ExposedCocktailList(filtered))

            ExposedCocktailList(filtered)
        }
    }
}