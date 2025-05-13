package com.zioanacleto.search

import kotlinx.serialization.Serializable

@Serializable
data class SearchRequest(
    val query: String
)

@Serializable
data class HuggingFaceSearchRequest(
    val inputs: String,
    val parameters: Parameters
)

@Serializable
data class Parameters(
    val candidate_labels: List<String>
)