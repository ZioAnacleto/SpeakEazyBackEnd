package ingredients.repository

import com.zioanacleto.ingredients.ExposedIngredient
import com.zioanacleto.ingredients.repository.IngredientsRepositoryImpl
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.Database
import org.junit.Before
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IngredientsRepositoryImplTest {

    private lateinit var database: Database
    private lateinit var repository: IngredientsRepositoryImpl

    @Before
    fun setup() {
        database = Database.connect(
            url = "jdbc:h2:mem:test_${UUID.randomUUID()};DB_CLOSE_DELAY=-1;",
            driver = "org.h2.Driver"
        )

        repository = IngredientsRepositoryImpl(database)
    }

    @Test
    fun `create inserts ingredient and returns id`() = runTest {
        val ingredient = ExposedIngredient(
            id = "",
            name = "Vodka",
            imageUrl = "vodka.png"
        )

        val id = repository.create(ingredient)

        assertTrue(id > 0)
    }

    @Test
    fun `readSingle returns null when ingredient does not exist`() = runTest {
        val result = repository.readSingle(999)

        assertNull(result)
    }

    @Test
    fun `readSingle returns inserted ingredient`() = runTest {
        val ingredient = ExposedIngredient(
            id = "",
            name = "Gin",
            imageUrl = "gin.png"
        )

        val id = repository.create(ingredient)

        val result = repository.readSingle(id)

        assertNotNull(result)
        assertEquals(id.toString(), result.id)
        assertEquals("Gin", result.name)
        assertEquals("gin.png", result.imageUrl)
    }

    @Test
    fun `readAll returns empty list when no data`() = runTest {
        val result = repository.readAll()

        assertTrue(result.ingredients.isEmpty())
    }

    @Test
    fun `readAll returns all inserted ingredients`() = runTest {
        val ingredient1 = ExposedIngredient("", "Rum", "rum.png")
        val ingredient2 = ExposedIngredient("", "Tequila", "tequila.png")

        repository.create(ingredient1)
        repository.create(ingredient2)

        val result = repository.readAll()

        assertEquals(2, result.ingredients.size)

        val names = result.ingredients.map { it.name }
        assertTrue(names.contains("Rum"))
        assertTrue(names.contains("Tequila"))
    }

    @Test
    fun `readAll maps fields correctly`() = runTest {
        val ingredient = ExposedIngredient("", "Whiskey", "whiskey.png")

        val id = repository.create(ingredient)

        val result = repository.readAll().ingredients.first()

        assertEquals(id.toString(), result.id)
        assertEquals("Whiskey", result.name)
        assertEquals("whiskey.png", result.imageUrl)
    }
}