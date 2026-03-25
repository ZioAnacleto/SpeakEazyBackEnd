package cocktails.repository

import com.zioanacleto.cocktails.ExposedCocktail
import com.zioanacleto.cocktails.ExposedCocktailIngredient
import com.zioanacleto.cocktails.ExposedCocktailIngredients
import com.zioanacleto.cocktails.ExposedCocktailTags
import com.zioanacleto.cocktails.repository.CocktailRepositoryImpl
import com.zioanacleto.ingredients.ExposedIngredient
import com.zioanacleto.ingredients.repository.IngredientsServiceMock
import com.zioanacleto.ingredients.service.IngredientsService
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.Database
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.testcontainers.containers.PostgreSQLContainer
import kotlin.test.Test

class CocktailRepositoryImplTest {
    private lateinit var database: Database
    private lateinit var repository: CocktailRepositoryImpl
    private lateinit var ingredientsService: IngredientsService
    private lateinit var postgres: PostgreSQLContainer<*>

    @Before
    fun setup() {
        postgres = PostgreSQLContainer("postgres:15").apply {
            start()
        }

        database = Database.connect(
            url = postgres.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = postgres.username,
            password = postgres.password
        )
        ingredientsService = IngredientsServiceMock()
        repository = CocktailRepositoryImpl(database, ingredientsService)
    }

    @After
    fun tearDown() {
        postgres.stop()
    }

    @Test
    fun `create inserts cocktail and returns id`() = runTest {
        val cocktail = fakeCocktail()

        val id = repository.create(
            cocktail,
            cocktailInstructions = "Step1. Step2",
            cocktailInstructionsIt = "Step1. Step2"
        )

        assertTrue(id > 0)
    }

    @Test
    fun `readSingle returns inserted cocktail`() = runTest {
        val cocktail = fakeCocktail()

        val id = repository.create(cocktail, "A. B", "A. B")

        val result = repository.readSingle(id)

        assertNotNull(result)
        assertEquals(cocktail.name, result?.name)
        assertEquals(cocktail.category, result?.category)
    }

    @Test
    fun `updateVisualizations increments value`() = runTest {
        val cocktail = fakeCocktail(visualizations = 5)

        val id = repository.create(cocktail, "A. B", "A. B")

        repository.updateVisualizations(id)

        val updated = repository.readSingle(id)

        assertEquals(6, updated?.visualizations?.toInt())
    }

    @Test
    fun `readAll returns all cocktails`() = runTest {
        repository.create(fakeCocktail(name = "A"), "A", "A")
        repository.create(fakeCocktail(name = "B"), "A", "A")

        val result = repository.readAll()

        assertEquals(2, result.cocktails.size)
    }

    @Test
    fun `readAllWithCategory filters correctly`() = runTest {
        repository.create(fakeCocktail(name = "A", category = "cat1"), "A", "A")
        repository.create(fakeCocktail(name = "B", category = "cat2"), "A", "A")

        val result = repository.readAllWithCategory("cat1")

        assertEquals(1, result.cocktails.size)
        assertEquals("cat1", result.cocktails.first().category)
    }

    @Test
    fun `readMostPopular returns sorted cocktails`() = runTest {
        repository.create(fakeCocktail(name = "A", visualizations = 1), "A", "A")
        repository.create(fakeCocktail(name = "B", visualizations = 10), "A", "A")

        val result = repository.readMostPopular(1)

        assertEquals(1, result.cocktails.size)
        assertEquals("B", result.cocktails.first().name)
    }

    @Test
    fun `readSingleWithName returns correct cocktail`() = runTest {
        repository.create(fakeCocktail(name = "Negroni"), "A", "A")

        val result = repository.readSingleWithName("Negroni")

        assertNotNull(result)
        assertEquals("Negroni", result?.name)
    }

    @Test
    fun `readSingle enriches ingredients from service`() = runTest {
        val ingredient = ExposedIngredient(
            id = "1",
            name = "Vodka",
            imageUrl = "vodka.png"
        )

        ingredientsService = IngredientsServiceMock().apply {
            ingredients = listOf(ingredient)
        }

        repository = CocktailRepositoryImpl(database, ingredientsService)

        val cocktail = fakeCocktail(
            ingredients = ExposedCocktailIngredients(
                listOf(
                    ExposedCocktailIngredient(
                        id = "1",
                        name = "",
                        imageUrl = "",
                        quantityCl = "10",
                        quantityOz = "5",
                        quantitySpecial = null
                    )
                )
            )
        )

        val id = repository.create(cocktail, "A", "A")

        val result = repository.readSingle(id)

        val enriched = result!!.ingredients.ingredients.first()

        assertEquals("Vodka", enriched.name)
        assertEquals("vodka.png", enriched.imageUrl)
    }

    private fun fakeCocktail(
        name: String = "Mojito",
        category: String = "Classic",
        visualizations: Long = 0,
        ingredients: ExposedCocktailIngredients = ExposedCocktailIngredients(emptyList())
    ) = ExposedCocktail(
        id = "",
        name = name,
        category = category,
        instructions = emptyList(),
        instructionsIt = emptyList(),
        glass = "Highball",
        isAlcoholic = true,
        imageLink = "img",
        videoLink = "vid",
        type = "drink",
        method = "shake",
        ingredients = ingredients,
        visualizations = visualizations,
        tags = ExposedCocktailTags(emptyList()),
        userId = "user",
        username = "username"
    )
}