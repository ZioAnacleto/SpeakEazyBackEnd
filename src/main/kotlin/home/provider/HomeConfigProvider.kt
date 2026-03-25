package com.zioanacleto.home.provider

import com.zioanacleto.home.HomeSectionsConfig

interface HomeConfigProvider {
    fun loadConfig(): HomeSectionsConfig
    fun updateConfig(newContent: HomeSectionsConfig)
}