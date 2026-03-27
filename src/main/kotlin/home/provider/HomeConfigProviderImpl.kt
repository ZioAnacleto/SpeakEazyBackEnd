package com.zioanacleto.home.provider

import com.zioanacleto.home.HomeSectionsConfig
import kotlinx.serialization.json.Json
import java.io.File

class HomeConfigProviderImpl : HomeConfigProvider {

    companion object {
        private const val FILE_NAME = "home_sections.json"
    }

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

    override fun loadConfig(): HomeSectionsConfig {
        return json.decodeFromString(configFile.readText())
    }

    override fun updateConfig(newContent: HomeSectionsConfig) {
        configFile.writeText(json.encodeToString(newContent))
    }
}