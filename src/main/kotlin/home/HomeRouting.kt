package com.zioanacleto.home

import com.zioanacleto.baseGetApi
import com.zioanacleto.setCacheControl
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Routing.setupHomeRouting() {
    val homeService: HomeService by inject()

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