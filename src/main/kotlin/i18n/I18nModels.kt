package com.zioanacleto.i18n

import kotlinx.serialization.Serializable

@Serializable
data class ExposedI18nRequest(
    val app: String,
    val strings: List<I18nKeyValueLanguage>
)

@Serializable
data class I18nKeyValueLanguage(
    val key: String,
    val value: String,
    val language: String
)

@Serializable
data class ExposedI18nResponse(
    val app: String,
    val languages: List<ExposedI18nResponseLanguages>
)

@Serializable
data class ExposedI18nResponseLanguages(
    val language: String,
    val strings: List<I18nKeyValue>
)

@Serializable
data class I18nKeyValue(
    val key: String,
    val value: String,
)

enum class Language(val code: String) {
    ENGLISH("en"),
    ITALIAN("it")
}