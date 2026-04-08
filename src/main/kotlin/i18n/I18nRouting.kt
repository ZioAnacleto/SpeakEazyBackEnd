package com.zioanacleto.i18n

import com.zioanacleto.baseGetApi
import com.zioanacleto.basePostApi
import com.zioanacleto.i18n.repository.I18nRepositoryImpl
import com.zioanacleto.i18n.service.I18nService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import org.koin.ktor.ext.inject

fun Routing.setupI18nRouting() {
    val i18nService: I18nService by inject()

    post("/i18n/add") {
        val request = call.receive<ExposedI18nRequest>()
        basePostApi(request) {
            val inserted = i18nService.insertBaseStrings(request)
            call.respond(HttpStatusCode.Created, inserted)

            inserted
        }
        // Background job to translate with AI new strings
        call.application.launch {
            i18nService.generateTranslationsAsync(request)
        }
    }

    get("/i18n/export") {
        baseGetApi {
            val export = i18nService.exportTranslations()
            call.respond(HttpStatusCode.OK, export)

            export
        }
    }

    get("/i18n/updates") {
        baseGetApi {
            val updates = i18nService.hasUpdates()
            call.respond(HttpStatusCode.OK, updates)

            updates
        }
    }

    post("/i18n/mark-published") {
        val publishedVersion = call.receiveText()
        basePostApi(publishedVersion) {
            val latestVersion = i18nService.getLatestUpdate()
            latestVersion?.let { i18nService.uploadMetadata(I18nRepositoryImpl.LAST_PUBLISHED_DATE, it) }
            i18nService.uploadMetadata(I18nRepositoryImpl.LAST_PUBLISHED_VERSION, publishedVersion)
            call.respond(HttpStatusCode.OK, latestVersion ?: "")

            latestVersion
        }
    }
}