package com.zioanacleto.ingredients

import kotlinx.serialization.Serializable

@Serializable
data class ExposedIngredients(
    val ingredients: List<ExposedIngredient>
)

@Serializable
data class ExposedIngredient(
    val id: String,
    val name: String,
    val imageUrl: String
)