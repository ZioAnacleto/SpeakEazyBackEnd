package com.zioanacleto.i18n.service

import com.zioanacleto.i18n.ExposedI18nRequest
import com.zioanacleto.i18n.ExposedI18nResponse
import com.zioanacleto.i18n.I18nUpdates

interface I18nService {
    suspend fun insertBaseStrings(request: ExposedI18nRequest): Int
    suspend fun generateTranslationsAsync(request: ExposedI18nRequest)
    suspend fun exportTranslations(): ExposedI18nResponse
    suspend fun hasUpdates(): I18nUpdates
    suspend fun getLatestUpdate(): String?
    suspend fun uploadMetadata(key: String, value: String)
}