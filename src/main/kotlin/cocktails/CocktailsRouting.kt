package com.zioanacleto.cocktails

import com.zioanacleto.*
import com.zioanacleto.cocktails.service.CocktailsService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject

fun Routing.setupCocktailsRouting() {
    val cocktailsService: CocktailsService by inject()

    // Add new cocktail
    post("/cocktails/add") {
        val log = call.application.log
        val cocktailText = call.receiveText()
        log.debug(cocktailText)
        val cocktail = Json.decodeFromString<ExposedCocktail>(cocktailText)
        log.debug("Adding cocktail: {}", cocktail)

        basePostApi(cocktail) {
            val id = cocktailsService.create(cocktail)
            call.respond(HttpStatusCode.Created, id)

            id
        }
    }

    // Get all cocktails
    get("/cocktails") {
        baseGetApi {
            val cocktails = cocktailsService.readAll()
            call.respond(HttpStatusCode.OK, cocktails)

            cocktails
        }
    }

    // Get single cocktail by id
    get("/cocktails/{id}") {
        baseGetApi {
            call.setCacheControl(CACHE_CONTROL_ONE_MINUTE)
            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
            val cocktail = cocktailsService.readSingle(id)

            cocktail?.let { returnedCocktail ->
                call.respond(HttpStatusCode.OK, returnedCocktail)
            } ?: run {
                call.respond(HttpStatusCode.NotFound)
            }

            cocktail
        }
    }

    // Updates single cocktail with new data, updating visualizations number
    put("/cocktails/{id}") {
        basePutApi {
            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")

            val updatedCocktails = cocktailsService.updateVisualizations(id)
            call.respond(HttpStatusCode.OK, updatedCocktails)

            updatedCocktails
        }
    }
}