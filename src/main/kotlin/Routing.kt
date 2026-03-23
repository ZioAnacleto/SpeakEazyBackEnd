package com.zioanacleto

import com.zioanacleto.admin.setupAdminRouting
import com.zioanacleto.cocktails.setupCocktailsRouting
import com.zioanacleto.home.setupHomeRouting
import com.zioanacleto.ingredients.setupIngredientsRouting
import com.zioanacleto.search.setupSearchRouting
import com.zioanacleto.tags.setupTagsRouting
import io.ktor.client.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Welcome to the SpeakEazy Backend!")
        }

        setupCocktailsRouting()
        setupIngredientsRouting()
        setupHomeRouting()
        setupSearchRouting()
        setupTagsRouting()
        setupAdminRouting()
    }
}
