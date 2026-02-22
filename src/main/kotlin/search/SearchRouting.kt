package com.zioanacleto.search

import com.zioanacleto.baseGetApi
import com.zioanacleto.basePostApi
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database

fun Routing.setupSearchRouting(database: Database) {
    val searchService = SearchService(database)

    // for HuggingFace LLM query
    post("/search") {
        val request = call.receive<SearchRequest>()
        basePostApi(request) {
            val cocktails = searchService.searchForCocktailsUsingHuggingFace(request)
            call.respond(HttpStatusCode.OK, cocktails)

            cocktails
        }
    }

    get("/search") {
        baseGetApi {
            val query = call.request.queryParameters["query"]

            println("nameQuery: $query")

            val response = searchService.searchForCocktails(query ?: "")
            call.respond(HttpStatusCode.OK, response)

            response
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

            val response = searchService.filterCocktails(nameQuery, ingredientQuery, tagQuery)
            call.respond(HttpStatusCode.OK, response)

            response
        }
    }
}