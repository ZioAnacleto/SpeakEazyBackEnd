package com.zioanacleto.search

import kotlinx.serialization.Serializable

@Serializable
data class SearchRequest(
    val query: String
) {
    fun toInputPrompt() = """{"inputs: "$query"}"""
}