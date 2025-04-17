package com.zioanacleto

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class ExposedIngredientsList(
    val ingredientsList: ExposedIngredients
)

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

class IngredientsService(database: Database) {
    object Ingredients : Table() {
        val id = integer(DB_KEY_ID)
        val name = varchar(DB_KEY_NAME, 500)
        val image = varchar(DB_KEY_IMAGE, 2000)
    }

    init {
        transaction {
            SchemaUtils.create(Ingredients)
        }
    }

    suspend fun create(ingredient: ExposedIngredient): Int = dbQuery {
        Ingredients.insert {
            it[id] = ingredient.id.toInt()
            it[name] = ingredient.name
            it[image] = ingredient.imageUrl
        }[Ingredients.id]
    }

    suspend fun readSingle(id: Int): ExposedIngredient? =
        dbQuery {
            Ingredients.selectAll()
                .where { Ingredients.id eq id }
                .map {
                    it.createIngredient()
                }.singleOrNull()
        }

    // todo: pagination?
    suspend fun readAll(): ExposedIngredientsList = dbQuery {
        ExposedIngredientsList(
            ingredientsList = ExposedIngredients(
                Ingredients.selectAll()
                    .mapNotNull { it.createIngredient() }
            )
        )
    }

    private fun ResultRow.createIngredient() =
        ExposedIngredient(
            id = this[Ingredients.id].toString(),
            name = this[Ingredients.name],
            imageUrl = this[Ingredients.image]
        )

    companion object {
        const val DB_KEY_ID = "id"
        const val DB_KEY_NAME = "name"
        const val DB_KEY_IMAGE = "image"
    }
}