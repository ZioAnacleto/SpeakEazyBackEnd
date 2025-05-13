package com.zioanacleto.tags

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database

fun Routing.setupTagsRouting(database: Database) {
    val tagsService = TagsService(database)

    get("/tags") {
        val tags = tagsService.readAll()
        call.respond(HttpStatusCode.OK, tags)
    }
}