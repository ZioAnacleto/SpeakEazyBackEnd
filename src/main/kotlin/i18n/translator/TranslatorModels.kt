package com.zioanacleto.i18n.translator

import kotlinx.serialization.Serializable

@Serializable
data class HuggingFaceSingleTextTranslationRequest(
    val inputs: String
)

@Serializable
data class HuggingFaceMultipleTextsRequest(
    val inputs: List<String>
)

@Suppress("PropertyName")
@Serializable
data class HuggingFaceMultipleTextsResponse(
    val translation_text: String
)