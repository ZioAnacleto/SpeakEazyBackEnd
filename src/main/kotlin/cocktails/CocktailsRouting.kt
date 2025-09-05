package com.zioanacleto.cocktails

import com.zioanacleto.baseDeleteApi
import com.zioanacleto.baseGetApi
import com.zioanacleto.basePostApi
import com.zioanacleto.basePutApi
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database

fun Routing.setupCocktailsRouting(database: Database) {
    val cocktailService = CocktailService(database)

    // Add new cocktail
    post("/cocktails/add") {
        val cocktail = try {
            call.receive<ExposedCocktail>()
        } catch (exception: Exception) {
            println("Error while adding cocktail: $exception")
            null
        }

        println("Adding cocktail: $cocktail")
        basePostApi(cocktail) {
            val id = cocktailService.create(cocktail!!)
            call.respond(HttpStatusCode.Created, id)

            id
        }
    }

    // Get all cocktails
    get("/cocktails") {
        baseGetApi {
            val cocktails = cocktailService.readAll()
            call.respond(HttpStatusCode.OK, cocktails)

            cocktails
        }
    }

    // Get single cocktail by id
    get("/cocktails/{id}") {
        baseGetApi {
            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
            val cocktail = cocktailService.readSingle(id)

            cocktail?.let { returnedCocktail ->
                call.respond(HttpStatusCode.OK, returnedCocktail)
            } ?: run {
                call.respond(HttpStatusCode.NotFound)
            }

            cocktail
        }
    }

    // Alter table deleting a column
    delete("/cocktails/dropColumn/{columnName}") {
        val columnName = call.parameters["columnName"] ?: throw IllegalArgumentException("Invalid column name")
        baseDeleteApi(columnName) {
            val deletedColumnName = cocktailService.deleteColumn(columnName)
            call.respond(HttpStatusCode.Accepted, deletedColumnName)

            deletedColumnName
        }
    }

    // Updates single cocktail with new data, updating visualizations number
    put("/cocktails/{id}") {
        basePutApi {
            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")

            val updatedCocktails = cocktailService.updateVisualizations(id)
            call.respond(HttpStatusCode.OK, updatedCocktails)

            updatedCocktails
        }
    }
}