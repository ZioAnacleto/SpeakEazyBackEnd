package com.zioanacleto.ingredients

import com.zioanacleto.baseGetApi
import com.zioanacleto.basePostApi
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database

fun Routing.setupIngredientsRouting(database: Database) {
    val ingredientsService = IngredientsService(database)

    // Add new ingredient
    post("ingredients/add") {
        basePostApi(Int::class) {
            val ingredient = call.receive<ExposedIngredient>()
            val id = ingredientsService.create(ingredient)
            call.respond(HttpStatusCode.Created, id)

            id
        }
    }

    // Get all ingredients
    get("/ingredients") {
        baseGetApi {
            val ingredients = ingredientsService.readAll()
            call.respond(HttpStatusCode.OK, ingredients)

            ingredients
        }
    }

    // Get single ingredient by id
    get("/ingredients/{id}") {
        baseGetApi {
            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
            val ingredient = ingredientsService.readSingle(id)

            ingredient?.let { returnedCocktail ->
                call.respond(HttpStatusCode.OK, returnedCocktail)
            } ?: run {
                call.respond(HttpStatusCode.NotFound)
            }

            ingredient
        }
    }
}