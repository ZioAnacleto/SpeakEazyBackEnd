package com.zioanacleto.search

import com.zioanacleto.cocktails.CocktailService
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database

fun Routing.setupSearchRouting(database: Database) {
    val searchService = SearchService(database)
    val cocktailService = CocktailService(database)

    post("/search") {
        val request = call.receive<SearchRequest>()
        val embeddings = searchService.queryModel(request)

        println("Search api called, embeddings result: $embeddings")
    }
}