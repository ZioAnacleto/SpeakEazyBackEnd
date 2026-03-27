package home

import com.zioanacleto.cocktails.ExposedCocktail
import com.zioanacleto.cocktails.ExposedCocktailIngredients
import com.zioanacleto.cocktails.ExposedCocktailList
import com.zioanacleto.cocktails.ExposedCocktailTags
import com.zioanacleto.cocktails.service.CocktailsService
import com.zioanacleto.home.HomeSectionsConfig
import com.zioanacleto.home.HomeService
import com.zioanacleto.home.SectionConfig
import com.zioanacleto.home.SectionType
import com.zioanacleto.home.provider.HomeConfigProvider
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class HomeServiceTest {
    private lateinit var cocktailsService: CocktailsService
    private lateinit var homeConfigProvider: HomeConfigProvider
    private lateinit var service: HomeService

    @Before
    fun setup() {
        cocktailsService = mockk()
        homeConfigProvider = mockk()

        service = HomeService(cocktailsService, homeConfigProvider)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `homeSections returns category section`() = runTest {
        val config = homeConfig(
            sections = listOf(
                section(type = SectionType.CATEGORY.typeName, query = "classic", title = "Classics")
            )
        )

        every { homeConfigProvider.loadConfig() } returns config

        coEvery {
            cocktailsService.readAllWithCategory("classic")
        } returns ExposedCocktailList(listOf(cocktail("Mojito")))

        val result = service.homeSections()

        assertEquals(1, result.sections.size)
        assertEquals("Classics", result.sections.first().name)
        assertEquals(1, result.sections.first().cocktails.size)
    }

    @Test
    fun `homeSections returns type section`() = runTest {
        every { homeConfigProvider.loadConfig() } returns homeConfig(
            listOf(section(type = SectionType.TYPE.typeName, query = "alcoholic", title = "Alcoholic"))
        )

        coEvery {
            cocktailsService.readAllWithType("alcoholic")
        } returns ExposedCocktailList(listOf(cocktail("Negroni")))

        val result = service.homeSections()

        assertEquals("Alcoholic", result.sections.first().name)
    }

    @Test
    fun `homeSections returns popular section`() = runTest {
        every { homeConfigProvider.loadConfig() } returns homeConfig(
            listOf(section(type = SectionType.POPULAR.typeName, limit = 2, title = "Popular"))
        )

        coEvery {
            cocktailsService.readMostPopular(2)
        } returns ExposedCocktailList(listOf(cocktail("A"), cocktail("B")))

        val result = service.homeSections()

        assertEquals(2, result.sections.first().cocktails.size)
    }

    @Test
    fun `homeSections returns ingredient section`() = runTest {
        every { homeConfigProvider.loadConfig() } returns homeConfig(
            listOf(section(type = SectionType.INGREDIENT.typeName, query = "vodka", title = "Vodka"))
        )

        coEvery {
            cocktailsService.readAllWithIngredient("vodka")
        } returns ExposedCocktailList(listOf(cocktail("Vodka Martini")))

        val result = service.homeSections()

        assertEquals("Vodka Martini", result.sections.first().cocktails.first().name)
    }

    @Test
    fun `homeSections returns banner`() = runTest {
        val bannerSection = section(
            type = SectionType.BANNER.typeName,
            query = "Mojito",
            title = "Featured",
            position = "1"
        )

        every { homeConfigProvider.loadConfig() } returns homeConfig(
            sections = listOf(bannerSection)
        )

        coEvery {
            cocktailsService.readSingleWithName("Mojito")
        } returns cocktail("Mojito")

        val result = service.homeSections()

        assertNotNull(result.banner)
        assertEquals("Featured", result.banner?.name)
    }

    @Test
    fun `homeSections returns null banner if cocktail not found`() = runTest {
        val bannerSection = section(
            type = SectionType.BANNER.typeName,
            query = "Unknown",
            title = "Featured",
            position = "1"
        )

        every { homeConfigProvider.loadConfig() } returns homeConfig(
            listOf(bannerSection)
        )

        coEvery {
            cocktailsService.readSingleWithName("Unknown")
        } returns null

        val result = service.homeSections()

        assertNull(result.banner)
    }

    @Test
    fun `homeSections returns multiple sections`() = runTest {
        every { homeConfigProvider.loadConfig() } returns homeConfig(
            listOf(
                section(type = SectionType.CATEGORY.typeName, query = "classic", title = "Classics"),
                section(type = SectionType.TYPE.typeName, query = "alcoholic", title = "Alcoholic")
            )
        )

        coEvery { cocktailsService.readAllWithCategory(any()) } returns ExposedCocktailList(emptyList())
        coEvery { cocktailsService.readAllWithType(any()) } returns ExposedCocktailList(emptyList())

        val result = service.homeSections()

        assertEquals(2, result.sections.size)
    }

    private fun homeConfig(sections: List<SectionConfig>) =
        HomeSectionsConfig(
            numberOfSections = sections.size,
            sections = sections
        )

    private fun section(
        type: String,
        query: String? = null,
        title: String = "",
        limit: Int? = null,
        position: String? = null,
        cta: String? = null
    ) = SectionConfig(
        type = type,
        query = query,
        title = title,
        limit = limit,
        position = position,
        cta = cta
    )

    private fun cocktail(name: String) = ExposedCocktail(
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
        ingredients = ExposedCocktailIngredients(emptyList()),
        visualizations = 0,
        tags = ExposedCocktailTags(emptyList()),
        userId = "",
        username = ""
    )
}