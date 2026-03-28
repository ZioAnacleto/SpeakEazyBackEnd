package com.zioanacleto.admin

interface EnvironmentKeysProvider {
    fun provideKey(key: EnvironmentKey): String
}

enum class EnvironmentKey(val key: String) {
    TRANSLATION_ENGLISH_ITALIAN("TRANSLATION_AI_URL_EN_IT"),
    TRANSLATION_ITALIAN_ENGLISH("TRANSLATION_AI_URL_IT_EN"),
    AI_TOKEN("AI_TOKEN"),
    SEARCH_AI_URL("AI_URL")
}