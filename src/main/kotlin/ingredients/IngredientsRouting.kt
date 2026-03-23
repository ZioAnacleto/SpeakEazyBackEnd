package com.zioanacleto.ingredients

import com.zioanacleto.baseGetApi
import com.zioanacleto.basePostApi
import com.zioanacleto.ingredients.service.IngredientsService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject

fun Routing.setupIngredientsRouting() {
    val ingredientsService: IngredientsService by inject()

    // Add new ingredient
    post("ingredients/add") {
        val ingredient = call.receive<ExposedIngredient>()
        val jsonIngredient = Json.encodeToString(ingredient)
        val log = call.application.log

        log.debug("Adding ingredient: $jsonIngredient")

        basePostApi(ingredient) {
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