package com.zioanacleto.search

data class SearchRequest(
    val query: String
) {
    fun toInputPrompt() = """{"inputs: "$query"}"""
}