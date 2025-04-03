package com.zioanacleto

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database

fun Application.configureRouting(database: Database) {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        setupCocktailsRouting(database)
    }
}

private fun Routing.setupCocktailsRouting(database: Database) {
    val cocktailService = CocktailService(database)

    // Add new cocktail
    post("/cocktails/add") {
        val cocktail = call.receive<ExposedCocktail>()
        val id = cocktailService.create(cocktail)
        call.respond(HttpStatusCode.Created, id)
    }

    // Get all cocktails
    get("/cocktails") {
        val cocktails = cocktailService.readAll()
        call.respond(HttpStatusCode.OK, cocktails)
    }

    // Get single cocktail by id
    get("/cocktails/{id}") {
        val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
        val cocktail = cocktailService.readSingle(id)

        cocktail?.let { returnedCocktail ->
            call.respond(HttpStatusCode.OK, returnedCocktail)
        } ?: run {
            call.respond(HttpStatusCode.NotFound)
        }
    }
}
