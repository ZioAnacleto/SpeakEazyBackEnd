package com.zioanacleto.i18n

import com.zioanacleto.basePostApi
import com.zioanacleto.i18n.service.I18nService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject

fun Routing.setupI18nRouting() {
    val i18nService: I18nService by inject()

    post("/i18n/add") {
        val log = call.application.log
        val requestText = call.receiveText()
        log.debug(requestText)

        val request = Json.decodeFromString<ExposedI18nRequest>(requestText)
        log.debug("Add strings: {}", request)

        basePostApi(request) {
            val counter = i18nService.insertStrings(request)
            call.respond(HttpStatusCode.Created, counter)

            counter
        }
    }
}