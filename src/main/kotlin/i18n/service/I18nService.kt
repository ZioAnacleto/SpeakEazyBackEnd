package com.zioanacleto.i18n.service

import com.zioanacleto.i18n.ExposedI18nRequest

interface I18nService {
    suspend fun insertBaseStrings(request: ExposedI18nRequest): Int
    suspend fun generateTranslationsAsync(request: ExposedI18nRequest)
}