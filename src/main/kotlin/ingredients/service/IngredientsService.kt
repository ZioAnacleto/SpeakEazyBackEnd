package com.zioanacleto.ingredients.service

import com.zioanacleto.ingredients.ExposedIngredient
import com.zioanacleto.ingredients.ExposedIngredients
import com.zioanacleto.ingredients.repository.IngredientsRepository

class IngredientsService(
    private val ingredientsRepository: IngredientsRepository
) {
    suspend fun create(ingredient: ExposedIngredient): Int =
        ingredientsRepository.create(ingredient)

    suspend fun readSingle(id: Int): ExposedIngredient? =
        ingredientsRepository.readSingle(id)

    suspend fun readAll(): ExposedIngredients =
        ingredientsRepository.readAll()
}