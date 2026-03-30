package com.zioanacleto.i18n

import kotlinx.serialization.Serializable

@Serializable
data class ExposedI18nRequest(
    val app: String,
    val strings: List<I18nKeyAndValue>
)

@Serializable
data class I18nKeyAndValue(
    val key: String,
    val value: String,
    val language: String
)