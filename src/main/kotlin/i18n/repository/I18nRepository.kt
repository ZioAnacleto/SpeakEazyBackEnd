package com.zioanacleto.i18n.repository

import com.zioanacleto.i18n.I18nKeyValueLanguage

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
    suspend fun getAllTranslationsFull(): List<I18nKeyValueLanguage>
    suspend fun getTranslationValue(key: String, language: String): String?
    suspend fun updateTranslation(
        keyTextId: String,
        translation: String,
        translationLanguage: String,
        currentDate: String
    ): Int

    suspend fun deleteTranslationsByKeyExceptLanguage(key: String, languageToKeep: String): Int
}