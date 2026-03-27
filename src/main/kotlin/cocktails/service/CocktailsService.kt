package com.zioanacleto.cocktails.service

import com.zioanacleto.cocktails.ExposedCocktail
import com.zioanacleto.cocktails.ExposedCocktailList

interface CocktailsService {
    suspend fun create(cocktail: ExposedCocktail): Int
    suspend fun updateVisualizations(cocktailId: Int): Int
    suspend fun readSingle(id: Int): ExposedCocktail?
    suspend fun readSingleWithName(name: String): ExposedCocktail?
    suspend fun readAllWithCategory(category: String): ExposedCocktailList
    suspend fun readAllWithType(type: String): ExposedCocktailList
    suspend fun readAllWithIngredient(ingredientName: String): ExposedCocktailList
    suspend fun readAll(): ExposedCocktailList
    suspend fun readMostPopular(size: Int): ExposedCocktailList
}