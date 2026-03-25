package com.zioanacleto.search.service

import com.zioanacleto.cocktails.ExposedCocktailList
import com.zioanacleto.search.SearchRequest

interface SearchService {
    suspend fun searchForCocktails(query: String): ExposedCocktailList
    suspend fun searchForCocktailsUsingHuggingFace(prompt: SearchRequest): ExposedCocktailList
    suspend fun filterCocktails(
        nameQuery: String?,
        ingredientsQuery: List<String> = emptyList(),
        tagsQuery: List<String> = emptyList()
    ): ExposedCocktailList
}