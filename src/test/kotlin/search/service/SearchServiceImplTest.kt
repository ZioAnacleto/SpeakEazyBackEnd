package search.service

import com.zioanacleto.admin.EnvironmentKeysProvider
import com.zioanacleto.cocktails.*
import com.zioanacleto.cocktails.service.CocktailsService
import com.zioanacleto.ingredients.ExposedIngredient
import com.zioanacleto.ingredients.ExposedIngredients
import com.zioanacleto.ingredients.service.IngredientsService
import com.zioanacleto.search.SearchRequest
import com.zioanacleto.search.service.SearchServiceImpl
import com.zioanacleto.tags.ExposedTag
import com.zioanacleto.tags.ExposedTagList
import com.zioanacleto.tags.service.TagsService
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SearchServiceImplTest {

    private lateinit var cocktailsService: CocktailsService
    private lateinit var ingredientsService: IngredientsService
    private lateinit var tagsService: TagsService
    private lateinit var httpClient: HttpClient
    private lateinit var keysProvider: EnvironmentKeysProvider

    @Before
    fun setup() {
        cocktailsService = mockk()
        ingredientsService = mockk()
        tagsService = mockk()
        keysProvider = mockk()
    }

    // --------------------------------
    // searchForCocktails
    // --------------------------------

    @Test
    fun `searchForCocktails filters by name`() = runTest {
        val cocktails = listOf(
            cocktail(name = "Mojito"),
            cocktail(name = "Negroni")
        )

        coEvery { cocktailsService.readAll() } returns ExposedCocktailList(cocktails)
        httpClient = mockk()

        val service = createSut()
        val result = service.searchForCocktails("moj")

        assertEquals(1, result.cocktails.size)
        assertEquals("Mojito", result.cocktails.first().name)
    }

    @Test
    fun `searchForCocktails filters by ingredient`() = runTest {
        val cocktails = listOf(
            cocktail(
                name = "Vodka Cocktail",
                ingredients = listOf(
                    ExposedCocktailIngredient(
                        id = "1",
                        name = "vodka"
                    )
                )
            ),
            cocktail(
                name = "Rum Cocktail",
                ingredients = listOf(
                    ExposedCocktailIngredient(
                        id = "2",
                        name = "rum"
                    )
                )
            )
        )

        coEvery { cocktailsService.readAll() } returns ExposedCocktailList(cocktails)
        httpClient = mockk()

        val service = createSut()
        val result = service.searchForCocktails("vodka")

        assertEquals(1, result.cocktails.size)
        assertEquals("Vodka Cocktail", result.cocktails.first().name)
    }

    // --------------------------------
    // filterCocktails
    // --------------------------------

    @Test
    fun `filterCocktails filters by name`() = runTest {
        val cocktails = listOf(
            cocktail(name = "Mojito"),
            cocktail(name = "Negroni")
        )

        coEvery { cocktailsService.readAll() } returns ExposedCocktailList(cocktails)
        coEvery { tagsService.readAll() } returns ExposedTagList(emptyList())
        httpClient = mockk()

        val service = createSut()
        val result = service.filterCocktails("moj", emptyList(), emptyList())

        assertEquals(1, result.cocktails.size)
    }

    @Test
    fun `filterCocktails filters by ingredients`() = runTest {
        val cocktails = listOf(
            cocktail(
                name = "Vodka Cocktail",
                ingredients = listOf(
                    ExposedCocktailIngredient(
                        id = "1",
                        name = "vodka"
                    )
                )
            ),
            cocktail(
                name = "Rum Cocktail",
                ingredients = listOf(
                    ExposedCocktailIngredient(
                        id = "2",
                        name = "rum"
                    )
                )
            )
        )

        coEvery { cocktailsService.readAll() } returns ExposedCocktailList(cocktails)
        coEvery { tagsService.readAll() } returns ExposedTagList(emptyList())
        httpClient = mockk()

        val service = createSut()
        val result = service.filterCocktails(null, listOf("vodka"), emptyList())

        assertEquals(1, result.cocktails.size)
        assertEquals("Vodka Cocktail", result.cocktails.first().name)
    }

    // --------------------------------
    // HuggingFace (mock HTTP)
    // --------------------------------

    @Test
    fun `searchForCocktailsUsingHuggingFace filters cocktails based on ingredient labels`() = runTest {
        val cocktails = listOf(
            cocktail(
                name = "Vodka Cocktail",
                ingredients = listOf(
                    ExposedCocktailIngredient(
                        id = "1",
                        name = "vodka"
                    )
                )
            ),
            cocktail(
                name = "Rum Cocktail",
                ingredients = listOf(
                    ExposedCocktailIngredient(
                        id = "2",
                        name = "rum"
                    )
                )
            )
        )

        val ingredients = listOf(
            ingredient("1", "vodka"),
            ingredient("2", "rum")
        )

        val tags = listOf(tag("1", "fresh"))

        coEvery { cocktailsService.readAll() } returns ExposedCocktailList(cocktails)
        coEvery { ingredientsService.readAll() } returns ExposedIngredients(ingredients)
        coEvery { tagsService.readAll() } returns ExposedTagList(tags)

        // Mock HTTP response
        val responseJson = """
            {
              "sequence": "",
              "labels": ["vodka", "rum"],
              "scores": [0.9, 0.1]
            }
        """.trimIndent()

        httpClient = mockHttpClient(
            status = HttpStatusCode.OK,
            responseJson
        )

        // env vars
        every { keysProvider.provideKey(any()) } returns "testLink"

        val service = createSut()
        val result = service.searchForCocktailsUsingHuggingFace(
            SearchRequest("strong drink")
        )

        assertEquals(1, result.cocktails.size)
        assertEquals("Vodka Cocktail", result.cocktails.first().name)
    }

    // --------------------------------
    // helpers
    // --------------------------------

    private fun createSut() = SearchServiceImpl(
        cocktailsService,
        ingredientsService,
        tagsService,
        httpClient,
        keysProvider
    )

    private fun mockHttpClient(
        status: HttpStatusCode,
        response: String
    ) = HttpClient(
        MockEngine { _ ->
            respond(
                status = status,
                content = response,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
    ) {
        install(ContentNegotiation) { json() }
    }

    private fun cocktail(
        name: String,
        ingredients: List<ExposedCocktailIngredient> = emptyList()
    ): ExposedCocktail {
        return ExposedCocktail(
            id = "1",
            name = name,
            category = "",
            instructions = emptyList(),
            instructionsIt = emptyList(),
            glass = "",
            isAlcoholic = true,
            imageLink = "",
            videoLink = "",
            type = "",
            method = "",
            ingredients = ExposedCocktailIngredients(ingredients),
            visualizations = 0,
            tags = ExposedCocktailTags(emptyList()),
            userId = "",
            username = ""
        )
    }

    private fun ingredient(id: String, name: String) =
        ExposedIngredient(id, name, "")

    private fun tag(id: String, name: String) = ExposedTag(id, name)

    // helper per env
    private fun setEnv(key: String, value: String) {
        System.setProperty(key, value)
    }
}