package com.zioanacleto.home

import io.ktor.server.application.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.atomic.AtomicReference

object HomeConfigManager {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    private val configCache = AtomicReference<HomeSectionsConfig>()
    private var lastModified: Long = 0L
    private var watchJob: Job? = null

    private val file = File("resources/home_sections.json")

    fun startAutoReload(scope: CoroutineScope, intervalMs: Long = INTERVAL) {
        println("startAutoReload called, loading file...")
        loadConfig()
        watchJob = scope.launch {
            while (isActive) {
                delay(intervalMs)
                val modified = file.lastModified()
                if (modified > lastModified) {
                    println("[HomeConfigManager] File modified, reloading configuration...")
                    loadConfig()
                }
            }
        }
    }

    fun getConfig(): HomeSectionsConfig =
        configCache.get() ?: HomeSectionsConfig(emptyList(), 0)

    fun stop() {
        watchJob?.cancel()
    }

    private fun loadConfig() {
        if (!file.exists()) {
            println("[HomeConfigManager] WARNING: configuration file not found, closing.")
            return
        }
        println("File exists, reading it...")
        val text = file.readText()
        val newConfig = json.decodeFromString(HomeSectionsConfig.serializer(), text)
        println("File read, newConfig: $newConfig")
        configCache.set(newConfig)
        println("Config cache is now populated: ${configCache.get()}")

        lastModified = file.lastModified()
        println("[HomeConfigManager] Configuration loaded with ${newConfig.sections.size} sections.")
    }

    private const val INTERVAL_MINUTES = 10
    private const val INTERVAL_SECONDS = 60
    private const val INTERVAL_MILLISECONDS = 1000L
    private const val INTERVAL = INTERVAL_MILLISECONDS * INTERVAL_SECONDS * INTERVAL_MINUTES
}

fun Application.configureHomeConfigManager() {
    val appScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    HomeConfigManager.startAutoReload(appScope)

    monitor.subscribe(ApplicationStopped) {
        HomeConfigManager.stop()
    }
}