package com.zioanacleto.i18n.translator

interface Translator {
    suspend fun translateSingleText(
        text: String,
        isFromEnglish: Boolean = true
    ): String

    suspend fun translateMultipleTexts(texts: List<String>): List<String>
}