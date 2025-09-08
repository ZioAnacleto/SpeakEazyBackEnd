package com.zioanacleto.cocktails

import kotlinx.serialization.Serializable

@Serializable
data class ExposedCocktailList(
    val cocktails: List<ExposedCocktail>
)

@Serializable
data class ExposedCocktailIngredients(
    val ingredients: List<ExposedCocktailIngredient>
)

@Serializable
data class ExposedCocktailIngredient(
    val id: String,
    var name: String = "",
    var imageUrl: String = "",
    var quantityCl: String = "",
    var quantityOz: String = "",
    var quantitySpecial: String? = null
)

@Serializable
data class ExposedCocktailTags(
    val tags: List<ExposedCocktailTag> = emptyList()
)

@Serializable
data class ExposedCocktailTag(
    val id: String
)

@Serializable
data class ExposedCocktail(
    val id: String,
    val name: String,
    val category: String,
    val instructions: String,
    val instructionsIt: String,
    val glass: String,
    val isAlcoholic: Boolean,
    val imageLink: String,
    val type: String,
    val method: String,
    var ingredients: ExposedCocktailIngredients,
    var visualizations: Long,
    var tags: ExposedCocktailTags,
    var userId: String,
    var username: String
) {
    override fun toString(): String {
        return "\nCocktail id: $id\nname: $name\ncategory: $category\nglass: $glass\ninstructions: $instructions\n" +
                "instructionsIT: $instructionsIt\nisAlcoholic: $isAlcoholic\nimageLink: $imageLink\n" +
                "type: $type\nmethod: $method\ningredients: $ingredients\nvisualizations: $visualizations\n" +
                "tags: $tags\nuserId: $userId\nusername: $username"
    }
}