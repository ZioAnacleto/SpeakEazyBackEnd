package com.zioanacleto.search

import com.zioanacleto.basePostApi
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database

fun Routing.setupSearchRouting(database: Database) {
    val searchService = SearchService(database)

    post("/search") {
        basePostApi {
            val request = call.receive<SearchRequest>()
            val cocktails = searchService.searchForCocktails(request)
            call.respond(HttpStatusCode.OK, cocktails)

            cocktails
        }
    }
}