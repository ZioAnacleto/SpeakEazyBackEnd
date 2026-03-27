package com.zioanacleto.ingredients.service

import com.zioanacleto.ingredients.ExposedIngredient
import com.zioanacleto.ingredients.ExposedIngredients
import com.zioanacleto.ingredients.repository.IngredientsRepository

class IngredientsServiceImpl(
    private val ingredientsRepository: IngredientsRepository
): IngredientsService {
    override suspend fun create(ingredient: ExposedIngredient): Int =
        ingredientsRepository.create(ingredient)

    override suspend fun readSingle(id: Int): ExposedIngredient? =
        ingredientsRepository.readSingle(id)

    override suspend fun readAll(): ExposedIngredients =
        ingredientsRepository.readAll()
}