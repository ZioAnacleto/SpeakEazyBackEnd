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
            call.respondText("Welcome to the SpeakEazy Backend!")
        }

        setupCocktailsRouting(database)
        setupIngredientsRouting(database)
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

    // Alter table deleting a column
    delete("/cocktails/dropColumn/{columnName}") {
        val columnName = call.parameters["columnName"] ?: throw IllegalArgumentException("Invalid column name")
        val deletedColumnName = cocktailService.deleteColumn(columnName)

        call.respond(HttpStatusCode.Accepted, deletedColumnName)
    }
}

private fun Routing.setupIngredientsRouting(database: Database) {
    val ingredientsService = IngredientsService(database)

    // Add new ingredient
    post("ingredients/add") {
        val ingredient = call.receive<ExposedIngredient>()
        val id = ingredientsService.create(ingredient)
        call.respond(HttpStatusCode.Created, id)
    }

    // Get all ingredients
    get("/ingredients") {
        val ingredients = ingredientsService.readAll()
        call.respond(HttpStatusCode.OK, ingredients)
    }

    // Get single ingredient by id
    get("/ingredients/{id}") {
        val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
        val ingredient = ingredientsService.readSingle(id)

        ingredient?.let { returnedCocktail ->
            call.respond(HttpStatusCode.OK, returnedCocktail)
        } ?: run {
            call.respond(HttpStatusCode.NotFound)
        }
    }
}
