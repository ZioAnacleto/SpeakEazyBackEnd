package com.zioanacleto.ingredients.service

import com.zioanacleto.ingredients.ExposedIngredient
import com.zioanacleto.ingredients.ExposedIngredients

interface IngredientsService {
    suspend fun create(ingredient: ExposedIngredient): Int
    suspend fun readSingle(id: Int): ExposedIngredient?
    suspend fun readAll(): ExposedIngredients
}