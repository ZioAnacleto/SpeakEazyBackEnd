package com.zioanacleto.i18n.repository

interface I18nRepository {
    suspend fun insertNewString(newTextId: String): Int
    suspend fun insertNewTranslation(
        keyTextId: String,
        translation: String,
        translationLanguage: String,
        currentDate: String
    ): Int
    suspend fun getAllTextIds(): List<String>
    suspend fun translationExists(key: String, language: String): Boolean
    suspend fun getAllTranslations(): List<Pair<String, String>>
    suspend fun markAsTranslatedIfComplete(key: String)
}