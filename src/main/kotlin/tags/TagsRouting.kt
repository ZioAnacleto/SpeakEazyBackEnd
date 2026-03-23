package com.zioanacleto.tags

import com.zioanacleto.baseGetApi
import com.zioanacleto.tags.service.TagsService
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Routing.setupTagsRouting() {
    val tagsService: TagsService by inject()

    get("/tags") {
        baseGetApi {
            val tags = tagsService.readAll()
            call.respond(HttpStatusCode.OK, tags)

            tags
        }
    }
}