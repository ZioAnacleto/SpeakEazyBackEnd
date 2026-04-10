package com.zioanacleto.cocktails.repository

import com.zioanacleto.cocktails.*
import com.zioanacleto.dbQuery
import com.zioanacleto.default
import com.zioanacleto.ingredients.service.IngredientsService
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.transactions.transaction

class CocktailRepositoryImpl(
    database: Database,
    private val ingredientsService: IngredientsService
) : CocktailsRepository {
    object Cocktails : Table() {
        val id = integer(DB_KEY_ID).autoIncrement().uniqueIndex()
        val name = varchar(DB_KEY_NAME, length = 500)
        val category = varchar(DB_KEY_CATEGORY, length = 500)
        val instructions = jsonb(DB_KEY_INSTRUCTIONS)
        val instructionsIt = jsonb(DB_KEY_INSTRUCTIONS_IT)
        val glass = varchar(DB_KEY_GLASS, length = 500)
        val isAlcoholic = bool(DB_KEY_IS_ALCOHOLIC)
        val imageLink = varchar(DB_KEY_IMAGE_LINK, length = 1000)
        val videoLink = varchar(DB_KEY_VIDEO_LINK, length = 1000)
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

            exec(
                """ 
                SELECT setval( 
                    'cocktails_id_seq', 
                    (SELECT COALESCE(MAX(id), 1) FROM cocktails),
                    true
                )
                """.trimIndent()
            )
        }
    }

    /**
     *  Suspend function that inserts new row on Cocktail DB, returning the newly inserted row's id
     */
    override suspend fun create(
        cocktail: ExposedCocktail,
        cocktailInstructions: String,
        cocktailInstructionsIt: String
    ): Int = dbQuery {
        Cocktails.insert {
            it[name] = cocktail.name
            it[category] = cocktail.category
            it[instructions] = Json.encodeToString(
                cocktailInstructions.trim().split(". ").mapToInstruction()
            )
            it[instructionsIt] = Json.encodeToString(
                cocktailInstructionsIt.trim().split(". ").mapToInstruction()
            )
            it[glass] = cocktail.glass
            it[isAlcoholic] = cocktail.isAlcoholic
            it[imageLink] = cocktail.imageLink
            it[videoLink] = cocktail.videoLink
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
    override suspend fun updateVisualizations(cocktailId: Int): Int =
        dbQuery {
            Cocktails.update({ Cocktails.id eq cocktailId }) {
                it[visualizations] = visualizations + 1
            }
        }

    /**
     *  Suspend function that returns a single cocktail with given id, with ingredients section enriched
     */
    override suspend fun readSingle(id: Int): ExposedCocktail? =
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
            val ingredients = ingredientsService.readAll()
                .ingredients
                .filter { ingredientsIds.contains(it.id.toInt()) }
                .map {
                    val cocktailIngredient = cocktail?.ingredients?.ingredients?.find { ing ->
                        ing.id == it.id
                    }

                    ExposedCocktailIngredient(
                        id = it.id,
                        name = it.name,
                        imageUrl = it.imageUrl,
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

    /**
     *  Suspend function that returns just a single cocktail with given name
     */
    override suspend fun readSingleWithName(name: String): ExposedCocktail? =
        dbQuery {
            // Find correct cocktail with given name
            Cocktails.selectAll()
                .where { Cocktails.name eq name }
                .map {
                    it.createCocktail()
                }
                .singleOrNull()
        }

    /**
     *  Suspend function that returns all cocktails with given category
     */
    override suspend fun readAllWithCategory(category: String): ExposedCocktailList =
        queryCocktailsWithWhereCondition { Cocktails.category eq category }

    /**
     *  Suspend function that returns all cocktails with given type
     */
    override suspend fun readAllWithType(type: String): ExposedCocktailList =
        queryCocktailsWithWhereCondition { Cocktails.type eq type }

    /**
     *  Suspend function that returns all cocktails that has ingredient with given name among all
     */
    override suspend fun readAllWithIngredient(ingredientName: String): ExposedCocktailList =
        dbQuery {
            val allIngredients = ingredientsService.readAll().ingredients
            ExposedCocktailList(
                // Retrieve all cocktails
                Cocktails.selectAll()
                    .mapNotNull {
                        it.createCocktail()
                    }
                    // filter out the ones without given ingredient
                    .filter { cocktail ->
                        val ingredientsIds =
                            cocktail.ingredients.ingredients.map { ingredient ->
                                ingredient.id.toInt()
                            }

                        val ingredients = allIngredients
                            .filter { ingredientsIds.contains(it.id.toInt()) && it.name == ingredientName }
                            .map { it.id }

                        ingredients.isNotEmpty()
                    }
            )
        }

    // todo: pagination?
    override suspend fun readAll(): ExposedCocktailList =
        dbQuery {
            val ingredientsMap = ingredientsService.readAll()
                .ingredients
                .associateBy { it.id }

            ExposedCocktailList(
                Cocktails.selectAll()
                    .mapNotNull {
                        val cocktail = it.createCocktail()

                        // Create ExposedCocktailIngredient objects from Ingredients table
                        val ingredients = cocktail.ingredients.ingredients
                            .mapNotNull cocktail@{ cocktailIngredient ->
                                val ingredient = ingredientsMap[cocktailIngredient.id] ?: return@cocktail null

                                ExposedCocktailIngredient(
                                    id = ingredient.id,
                                    name = ingredient.name,
                                    imageUrl = ingredient.imageUrl,
                                    quantityCl = cocktailIngredient.quantityCl.default("-"),
                                    quantityOz = cocktailIngredient.quantityOz.default("-"),
                                    quantitySpecial = cocktailIngredient.quantitySpecial
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

    /**
     *  Suspend function that returns a list [size] sized sorted by visualizations number
     *  in reverse order
     */
    override suspend fun readMostPopular(size: Int): ExposedCocktailList =
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
            instructions = Json.decodeFromString(this[Cocktails.instructions]),
            instructionsIt = Json.decodeFromString(this[Cocktails.instructionsIt]),
            glass = this[Cocktails.glass],
            isAlcoholic = this[Cocktails.isAlcoholic],
            imageLink = this[Cocktails.imageLink],
            videoLink = this[Cocktails.videoLink],
            type = this[Cocktails.type],
            method = this[Cocktails.method],
            ingredients = Json.decodeFromString(this[Cocktails.ingredients]),
            visualizations = this[Cocktails.visualizations],
            tags = Json.decodeFromString(this[Cocktails.tags]),
            userId = this[Cocktails.userId],
            username = username
        )
    }

    private fun List<String>.mapToInstruction() = this.map {
        ExposedCocktailInstruction(
            type = DEFAULT_INSTRUCTION_TYPE,
            instruction = it
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
        private const val DB_KEY_VIDEO_LINK = "videolink"
        private const val DB_KEY_TYPE = "type"
        private const val DB_KEY_METHOD = "method"
        private const val DB_KEY_INGREDIENTS = "ingredients"
        private const val DB_KEY_VISUALIZATIONS = "visualizations"
        private const val DB_KEY_TAGS = "tags"
        private const val DB_KEY_USER_ID = "userid"
        private const val DB_KEY_USERNAME = "username"
        private const val DEFAULT_INSTRUCTION_TYPE = "instruction"
    }
}