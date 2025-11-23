package com.zioanacleto.admin

import com.zioanacleto.baseGetApi
import com.zioanacleto.basePostApi
import com.zioanacleto.home.HomeConfigManager
import com.zioanacleto.home.HomeSectionsConfig
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun Routing.setupAdminRouting() {
    val config = HomeConfigManager.loadConfig()
    val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    get("admin/home-config") {
        baseGetApi {
            call.respondText(
                text = json.encodeToString(config),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        }
    }

    post("admin/home-config") {
        val receivedConfig = call.receive<HomeSectionsConfig>()
        basePostApi(receivedConfig) {
            HomeConfigManager.updateConfig(receivedConfig)
            call.respond(HttpStatusCode.OK, "Config updated")
        }
    }
}