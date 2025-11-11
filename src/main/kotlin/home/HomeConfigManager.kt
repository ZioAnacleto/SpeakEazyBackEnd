package com.zioanacleto.home

import io.ktor.server.application.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicReference

// todo: storing file outside and refresh using the watchJob
object HomeConfigManager {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    private val configCache = AtomicReference<HomeSectionsConfig>()
    private var lastModified: Long = 0L
    private var watchJob: Job? = null

    fun startAutoReload(scope: CoroutineScope, intervalMs: Long = INTERVAL) {
        println("startAutoReload called, loading file...")
        loadConfig()
        /*watchJob = scope.launch {
            while (isActive) {
                delay(intervalMs)
                val modified = file.lastModified()
                if (modified > lastModified) {
                    println("[HomeConfigManager] File modified, reloading configuration...")
                    loadConfig()
                }
            }
        }*/
    }

    fun getConfig(): HomeSectionsConfig =
        configCache.get() ?: HomeSectionsConfig(emptyList(), 0)

    fun stop() {
        watchJob?.cancel()
    }

    private fun loadConfig() {
        val resource = this::class.java.classLoader.getResourceAsStream("home_sections.json")
        checkNotNull(resource) {
            println("[HomeConfigManager] WARNING: configuration file not found, closing.")
            return
        }
        println("File exists, reading it...")
        val text = resource.bufferedReader().use { it.readText() }
        val newConfig = json.decodeFromString(HomeSectionsConfig.serializer(), text)
        println("File read, newConfig: $newConfig")
        configCache.set(newConfig)

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