package com.zioanacleto.home

import com.zioanacleto.baseGetApi
import com.zioanacleto.setCacheControl
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database

fun Routing.setupHomeRouting(database: Database) {
    val homeService = HomeService(database)

    // Get home sections containing cocktails
    get("/home") {
        baseGetApi {
            call.setCacheControl()
            val sections = homeService.homeSections()
            call.respond(
                HttpStatusCode.OK,
                sections
            )

            sections
        }
    }
}