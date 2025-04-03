package com.example

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class ExposedCocktail(
    val id: String,
    val name: String,
    val category: String,
    val instructions: String,
    val instructionsIt: String,
    val glass: String,
    val isAlcoholic: Boolean,
    val imageLink: String
) {
    override fun toString(): String {
        return "Cocktail id: $id\n name: $name\ncategory: $category\nglass: $glass\ninstructions: $instructions\n" +
                "instructionsIT: $instructionsIt\nisAlcoholic: $isAlcoholic\nimageLink: $imageLink"
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
        }[Cocktails.id]
    }

    suspend fun readSingle(id: Int): ExposedCocktail? =
        dbQuery {
            Cocktails.selectAll()
                .where { Cocktails.id eq id }
                .map {
                    ExposedCocktail(
                        id = it[Cocktails.id].toString(),
                        name = it[Cocktails.name],
                        category = it[Cocktails.category],
                        instructions = it[Cocktails.instructions],
                        instructionsIt = it[Cocktails.instructionsIt],
                        glass = it[Cocktails.glass],
                        isAlcoholic = it[Cocktails.isAlcoholic],
                        imageLink = it[Cocktails.imageLink]
                    )
                }
                .singleOrNull()
        }

    // todo: pagination?
    suspend fun readAll(): List<ExposedCocktail> =
        dbQuery {
            Cocktails.selectAll()
                .mapNotNull {
                    ExposedCocktail(
                        id = it[Cocktails.id].toString(),
                        name = it[Cocktails.name],
                        category = it[Cocktails.category],
                        instructions = it[Cocktails.instructions],
                        instructionsIt = it[Cocktails.instructionsIt],
                        glass = it[Cocktails.glass],
                        isAlcoholic = it[Cocktails.isAlcoholic],
                        imageLink = it[Cocktails.imageLink]
                    )
                }
        }

    companion object {
        const val DB_KEY_ID = "id"
        const val DB_KEY_NAME = "name"
        const val DB_KEY_CATEGORY = "category"
        const val DB_KEY_INSTRUCTIONS = "instructions"
        const val DB_KEY_INSTRUCTIONS_IT = "instructionsit"
        const val DB_KEY_GLASS = "glass"
        const val DB_KEY_IS_ALCOHOLIC = "isalcoholic"
        const val DB_KEY_IMAGE_LINK = "imagelink"
    }
}