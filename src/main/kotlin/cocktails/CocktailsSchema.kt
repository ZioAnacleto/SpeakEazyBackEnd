package com.zioanacleto.cocktails

import com.zioanacleto.dbQuery
import com.zioanacleto.default
import com.zioanacleto.ingredients.IngredientsService
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.transactions.transaction

class CocktailService(database: Database) {
    object Cocktails : Table() {
        val id = integer(DB_KEY_ID).autoIncrement()
        val name = varchar(DB_KEY_NAME, length = 500)
        val category = varchar(DB_KEY_CATEGORY, length = 500)
        val instructions = varchar(DB_KEY_INSTRUCTIONS, length = 500)
        val instructionsIt = varchar(DB_KEY_INSTRUCTIONS_IT, length = 500)
        val glass = varchar(DB_KEY_GLASS, length = 500)
        val isAlcoholic = bool(DB_KEY_IS_ALCOHOLIC)
        val imageLink = varchar(DB_KEY_IMAGE_LINK, length = 1000)
        val type = varchar(DB_KEY_TYPE, length = 500)
        val method = varchar(DB_KEY_METHOD, length = 500)
        val ingredients = jsonb(DB_KEY_INGREDIENTS)
        val visualizations = long(DB_KEY_VISUALIZATIONS)
        val tags = jsonb(DB_KEY_TAGS)
        val userId = varchar(DB_KEY_USER_ID, length = 500)
        val username = varchar(DB_KEY_USERNAME, length = 500)

        override val primaryKey: PrimaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Cocktails)

            // Not sure about cocktails_id_seq name
            exec("ALTER SEQUENCE cocktails_id_seq RESTART WITH 1000")
        }
    }

    /**
     *  Suspend function that inserts new row on Cocktail DB, returning the newly inserted row's id
     */
    suspend fun create(cocktail: ExposedCocktail): Int = dbQuery {
        val cocktailInstructions =
            cocktail.instructions.ifEmpty {
                InstructionsTranslator()
                    .translate(
                        text = cocktail.instructionsIt,
                        isFromEnglish = false
                    )
            }
        println("Create function, english instructions: $cocktailInstructions")

        val cocktailInstructionsIt =
            cocktail.instructionsIt.ifEmpty {
                InstructionsTranslator()
                    .translate(
                        text = cocktail.instructions
                    )
            }
        println("Create function, italian instructions: $cocktailInstructionsIt")

        Cocktails.insert {
            it[name] = cocktail.name
            it[category] = cocktail.category
            it[instructions] = cocktailInstructions
            it[instructionsIt] = cocktailInstructionsIt
            it[glass] = cocktail.glass
            it[isAlcoholic] = cocktail.isAlcoholic
            it[imageLink] = cocktail.imageLink
            it[type] = cocktail.type
            it[method] = cocktail.method
            it[tags] = Json.encodeToString(cocktail.tags)
            it[ingredients] = Json.encodeToString(cocktail.ingredients)
            it[visualizations] = cocktail.visualizations
            it[userId] = cocktail.userId
            it[username] = cocktail.username
        }[Cocktails.id]
    }

    /**
     *  Suspend function that updates matching row with new visualizations data in Cocktail DB
     */
    suspend fun updateVisualizations(cocktailId: Int): Int =
        dbQuery {
            Cocktails.update({ Cocktails.id eq cocktailId }) {
                it[visualizations] = visualizations + 1
            }
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
                    val cocktailIngredient = cocktail?.ingredients?.ingredients?.find { ing ->
                        ing.id == it[IngredientsService.Ingredients.id].toString()
                    }

                    ExposedCocktailIngredient(
                        id = it[IngredientsService.Ingredients.id].toString(),
                        name = it[IngredientsService.Ingredients.name],
                        imageUrl = it[IngredientsService.Ingredients.image],
                        quantityCl = cocktailIngredient?.quantityCl.default("-"),
                        quantityOz = cocktailIngredient?.quantityOz.default("-"),
                        quantitySpecial = cocktailIngredient?.quantitySpecial
                    )
                }

            // Mapping ingredients information inside cocktail object
            cocktail?.apply {
                this.ingredients = ExposedCocktailIngredients(
                    ingredients = ingredients
                )
            }

            cocktail
        }

    suspend fun readAllWithCategory(category: String): ExposedCocktailList =
        queryCocktailsWithWhereCondition { Cocktails.category eq category }

    suspend fun readAllWithType(type: String): ExposedCocktailList =
        queryCocktailsWithWhereCondition { Cocktails.type eq type }

    suspend fun readAllWithIngredient(ingredientName: String): ExposedCocktailList =
        readAll().apply {
            cocktails.filter { cocktail ->
                cocktail.ingredients.ingredients.any { ing -> ing.name.equals(ingredientName, ignoreCase = true) }
            }
        }

    // todo: pagination?
    suspend fun readAll(): ExposedCocktailList =
        dbQuery {
            ExposedCocktailList(
                Cocktails.selectAll()
                    .mapNotNull {
                        val cocktail = it.createCocktail()

                        // Select ingredients' ids of selected Cocktail
                        val ingredientsIds =
                            cocktail.ingredients.ingredients.map { ingredient -> ingredient.id.toInt() }

                        // Create ExposedCocktailIngredient objects from Ingredients table
                        val ingredients = IngredientsService.Ingredients.selectAll()
                            .where { IngredientsService.Ingredients.id inList ingredientsIds }
                            .map {
                                val cocktailIngredient = cocktail.ingredients.ingredients.find { ing ->
                                    ing.id == it[IngredientsService.Ingredients.id].toString()
                                }

                                ExposedCocktailIngredient(
                                    id = it[IngredientsService.Ingredients.id].toString(),
                                    name = it[IngredientsService.Ingredients.name],
                                    imageUrl = it[IngredientsService.Ingredients.image],
                                    quantityCl = cocktailIngredient?.quantityCl.default("-"),
                                    quantityOz = cocktailIngredient?.quantityOz.default("-"),
                                    quantitySpecial = cocktailIngredient?.quantitySpecial
                                )
                            }

                        // Mapping ingredients information inside cocktail object
                        cocktail.apply {
                            this.ingredients = ExposedCocktailIngredients(
                                ingredients = ingredients
                            )
                        }

                        cocktail
                    }
            )
        }

    suspend fun readMostPopular(size: Int): ExposedCocktailList =
        dbQuery {
            ExposedCocktailList(
                Cocktails.selectAll()
                    .mapNotNull {
                        it.createCocktail()
                    }
                    .sortedByDescending { it.visualizations }
                    .take(size)
            )
        }

    fun deleteColumn(columnName: String): String =
        transaction {
            val column = Column(Cocktails, columnName, IntegerColumnType())

            column.dropStatement().forEach {
                exec(it)
            }
            column.name
        }

    private suspend fun queryCocktailsWithWhereCondition(
        whereCondition: SqlExpressionBuilder.() -> Op<Boolean>
    ) = dbQuery {
        ExposedCocktailList(
            Cocktails.selectAll()
                .where(whereCondition)
                .mapNotNull {
                    it.createCocktail()
                }
        )
    }

    private fun ResultRow.createCocktail(): ExposedCocktail {
        val username = try {
            this[Cocktails.username] ?: "" // WARNING: do not remove this elvis
        } catch (exception: NullPointerException) {
            println("Exception caught, returning empty value.")
            ""
        }

        return ExposedCocktail(
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
            ingredients = Json.decodeFromString(this[Cocktails.ingredients]),
            visualizations = this[Cocktails.visualizations],
            tags = Json.decodeFromString(this[Cocktails.tags]),
            userId = this[Cocktails.userId],
            username = username
        )
    }

    companion object {
        private const val DB_KEY_ID = "id"
        private const val DB_KEY_NAME = "name"
        private const val DB_KEY_CATEGORY = "category"
        private const val DB_KEY_INSTRUCTIONS = "instructions"
        private const val DB_KEY_INSTRUCTIONS_IT = "instructionsit"
        private const val DB_KEY_GLASS = "glass"
        private const val DB_KEY_IS_ALCOHOLIC = "isalcoholic"
        private const val DB_KEY_IMAGE_LINK = "imagelink"
        private const val DB_KEY_TYPE = "type"
        private const val DB_KEY_METHOD = "method"
        private const val DB_KEY_INGREDIENTS = "ingredients"
        private const val DB_KEY_VISUALIZATIONS = "visualizations"
        private const val DB_KEY_TAGS = "tags"
        private const val DB_KEY_USER_ID = "userid"
        private const val DB_KEY_USERNAME = "username"
    }
}