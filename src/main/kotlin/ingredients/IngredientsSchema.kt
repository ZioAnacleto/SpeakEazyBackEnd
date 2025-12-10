package com.zioanacleto.ingredients

import com.zioanacleto.dbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class IngredientsService(database: Database) {
    object Ingredients : Table() {
        val id = integer(DB_KEY_ID).autoIncrement().uniqueIndex()
        val name = varchar(DB_KEY_NAME, 500)
        val image = varchar(DB_KEY_IMAGE, 2000)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Ingredients)
        }
    }

    suspend fun create(ingredient: ExposedIngredient): Int = dbQuery {
        Ingredients.insert {
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
    suspend fun readAll(): ExposedIngredients = dbQuery {
        ExposedIngredients(
            Ingredients.selectAll()
                .mapNotNull { it.createIngredient() }
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