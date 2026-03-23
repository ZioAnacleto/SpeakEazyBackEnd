package com.zioanacleto.search

import com.zioanacleto.baseGetApi
import com.zioanacleto.basePostApi
import com.zioanacleto.cocktails.ExposedCocktailList
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database
import org.koin.ktor.ext.inject

fun Routing.setupSearchRouting() {
    val searchService: SearchService by inject()

    suspend fun RoutingCall.checkResponseAndReturn(response: ExposedCocktailList) =
        with(response) {
            if (cocktails.isEmpty())
                respond(HttpStatusCode.NoContent, this)
            else
                respond(HttpStatusCode.OK, this)
        }

    // for HuggingFace LLM query
    post("/search") {
        val request = call.receive<SearchRequest>()
        basePostApi(request) {
            val cocktails = searchService.searchForCocktailsUsingHuggingFace(request)
            call.checkResponseAndReturn(cocktails)

            cocktails
        }
    }

    get("/search") {
        baseGetApi {
            val log = call.application.log
            val query = call.request.queryParameters["query"]

            log.debug("nameQuery: $query")

            val response = searchService.searchForCocktails(query ?: "")
            call.checkResponseAndReturn(response)

            response
        }
    }

    get("/search/filter") {
        baseGetApi {
            val log = call.application.log
            val nameQuery = call.request.queryParameters["name"]
            val ingredientQuery = call.request.queryParameters.getAll("ingredient") ?: emptyList()
            val tagQuery = call.request.queryParameters.getAll("tag") ?: emptyList()

            log.debug("nameQuery: $nameQuery")
            log.debug("ingredientQuery: {}", ingredientQuery)
            log.debug("tagQuery: {}", tagQuery)

            val response = searchService.filterCocktails(nameQuery, ingredientQuery, tagQuery)
            call.checkResponseAndReturn(response)

            response
        }
    }
}