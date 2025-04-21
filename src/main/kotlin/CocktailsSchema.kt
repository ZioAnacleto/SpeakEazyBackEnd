package com.zioanacleto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.internal.readJson
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

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
    var quantityOz: String = ""
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
    var ingredients: ExposedCocktailIngredients
) {
    override fun toString(): String {
        return "Cocktail id: $id\n name: $name\ncategory: $category\nglass: $glass\ninstructions: $instructions\n" +
                "instructionsIT: $instructionsIt\nisAlcoholic: $isAlcoholic\nimageLink: $imageLink\n" +
                "type: $type\nmethod: $method\ningredients: $ingredients"
    }
}

class CocktailService(database: Database) {
    object Cocktails : Table() {
        val id = integer(DB_KEY_ID)
        val name = varchar(DB_KEY_NAME, length = 500)
        val category = varchar(DB_KEY_CATEGORY, length = 500)
        val instructions = varchar(DB_KEY_INSTRUCTIONS, length = 500)
        val instructionsIt = varchar(DB_KEY_INSTRUCTIONS_IT, length = 500)
        val glass = varchar(DB_KEY_GLASS, length = 500)
        val isAlcoholic = bool(DB_KEY_IS_ALCOHOLIC)
        val imageLink = varchar(DB_KEY_IMAGE_LINK, length = 1000)
        val type = varchar(DB_KEY_TYPE, length = 500)
        val method = varchar(DB_KEY_METHOD, length = 500)
        val ingredients = largeText(DB_KEY_INGREDIENTS)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Cocktails)
        }
    }

    suspend fun create(cocktail: ExposedCocktail): Int = dbQuery {
        Cocktails.insert {
            it[id] = cocktail.id.toInt()
            it[name] = cocktail.name
            it[category] = cocktail.category
            it[instructions] = cocktail.instructions
            it[instructionsIt] = cocktail.instructionsIt
            it[glass] = cocktail.glass
            it[isAlcoholic] = cocktail.isAlcoholic
            it[imageLink] = cocktail.imageLink
            it[type] = cocktail.type
            it[method] = cocktail.method
            it[ingredients] = Json.encodeToString(cocktail.ingredients)
        }[Cocktails.id]
    }

    suspend fun readSingle(id: Int): ExposedCocktail? =
        dbQuery {
            // Find correct cocktail with given id
            val cocktail = Cocktails.selectAll()
                .where { Cocktails.id eq id }
                .map {
                    it.createCocktail()
                }
                .singleOrNull()

            // Select ingredients' ids of selected Cocktail
            val ingredientsIds = cocktail?.ingredients?.ingredients?.map { it.id.toInt() } ?: listOf()
            // Create ExposedCocktailIngredient objects from Ingredients table
            val ingredients = IngredientsService.Ingredients.selectAll()
                .where { IngredientsService.Ingredients.id inList ingredientsIds }
                .map {
                    ExposedCocktailIngredient(
                        id = it[IngredientsService.Ingredients.id].toString(),
                        name = it[IngredientsService.Ingredients.name],
                        imageUrl = it[IngredientsService.Ingredients.image]
                    )
                }

            // Mapping ingredients information inside cocktail object
            cocktail?.apply {
                this.ingredients.ingredients.map { cocktailIngredient ->
                    val actualIngredient = ingredients.find { it.id == cocktailIngredient.id }
                    ExposedCocktailIngredient(
                        id = cocktailIngredient.id,
                        name = actualIngredient?.name ?: "",
                        imageUrl = actualIngredient?.imageUrl ?: "",
                        quantityOz = cocktailIngredient.quantityOz,
                        quantityCl = cocktailIngredient.quantityCl
                    )
                }
            }
        }

    // todo: pagination?
    suspend fun readAll(): ExposedCocktailList =
        dbQuery {
            ExposedCocktailList(
                Cocktails.selectAll()
                    .mapNotNull {
                        it.createCocktail()
                    }
            )
        }

    private fun ResultRow.createCocktail() =
        ExposedCocktail(
            id = this[Cocktails.id].toString(),
            name = this[Cocktails.name],
            category = this[Cocktails.category],
            instructions = this[Cocktails.instructions],
            instructionsIt = this[Cocktails.instructionsIt],
            glass = this[Cocktails.glass],
            isAlcoholic = this[Cocktails.isAlcoholic],
            imageLink = this[Cocktails.imageLink],
            type = this[Cocktails.type],
            method = this[Cocktails.method],
            ingredients = Json.decodeFromString(this[Cocktails.ingredients])
        )

    companion object {
        const val DB_KEY_ID = "id"
        const val DB_KEY_NAME = "name"
        const val DB_KEY_CATEGORY = "category"
        const val DB_KEY_INSTRUCTIONS = "instructions"
        const val DB_KEY_INSTRUCTIONS_IT = "instructionsit"
        const val DB_KEY_GLASS = "glass"
        const val DB_KEY_IS_ALCOHOLIC = "isalcoholic"
        const val DB_KEY_IMAGE_LINK = "imagelink"
        const val DB_KEY_TYPE = "type"
        const val DB_KEY_METHOD = "method"
        const val DB_KEY_INGREDIENTS = "ingredients"
    }
}