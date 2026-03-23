package com.zioanacleto.ingredients.repository

import com.zioanacleto.ingredients.ExposedIngredient
import com.zioanacleto.ingredients.ExposedIngredients

interface IngredientsRepository {
    suspend fun create(ingredient: ExposedIngredient): Int
    suspend fun readSingle(id: Int): ExposedIngredient?
    suspend fun readAll(): ExposedIngredients
}