package com.zioanacleto.home

import kotlinx.serialization.json.Json
import java.io.File

object HomeConfigManager {
    private const val FILE_NAME = "home_sections.json"
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    // Editable File on filesystem
    private val configFile = File("data/$FILE_NAME")

    init {
        // We create folder if it doesn't exist
        configFile.parentFile.mkdirs()

        // If file doesn't exist, let's copy it from classpath
        if (!configFile.exists()) {
            javaClass.classLoader.getResourceAsStream(FILE_NAME)!!.use { input ->
                configFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    fun loadConfig(): HomeSectionsConfig {
        return json.decodeFromString(configFile.readText())
    }

    fun updateConfig(newContent: HomeSectionsConfig) {
        configFile.writeText(json.encodeToString(newContent))
    }
}