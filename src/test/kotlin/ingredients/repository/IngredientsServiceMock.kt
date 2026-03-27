package com.zioanacleto.ingredients.repository

import com.zioanacleto.ingredients.ExposedIngredient
import com.zioanacleto.ingredients.ExposedIngredients
import com.zioanacleto.ingredients.service.IngredientsService

class IngredientsServiceMock: IngredientsService {
    var ingredient: ExposedIngredient? = null
    var ingredients: List<ExposedIngredient> = emptyList()

    override suspend fun create(ingredient: ExposedIngredient): Int = 1

    override suspend fun readSingle(id: Int): ExposedIngredient? = ingredient

    override suspend fun readAll(): ExposedIngredients = ExposedIngredients(ingredients)
}