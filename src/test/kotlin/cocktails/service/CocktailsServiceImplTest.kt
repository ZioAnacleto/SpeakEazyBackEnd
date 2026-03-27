package cocktails.service

import com.zioanacleto.cocktails.*
import com.zioanacleto.cocktails.repository.CocktailsRepository
import com.zioanacleto.cocktails.service.CocktailsServiceImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import kotlin.test.Test

class CocktailsServiceImplTest {

    private val repository: CocktailsRepository = mockk()
    private val translator: InstructionsTranslator = mockk()

    private lateinit var service: CocktailsServiceImpl

    @Before
    fun setup() {
        service = CocktailsServiceImpl(repository, translator)
    }

    @Test
    fun `create uses provided english instructions when present`() = runTest {
        val cocktail = mockCocktail(
            instructions = listOf("step1", "step2"),
            instructionsIt = listOf("passo1")
        )

        coEvery { repository.create(any(), any(), any()) } returns 1

        val result = service.create(cocktail)

        coVerify {
            repository.create(
                cocktail,
                "step1 step2",
                "passo1"
            )
        }
        coVerify(exactly = 0) { translator.translate(any(), any()) }

        assertEquals(1, result)
    }

    @Test
    fun `create translates english instructions when missing`() = runTest {
        val cocktail = mockCocktail(
            instructions = emptyList(),
            instructionsIt = listOf("passo1", "passo2")
        )

        coEvery {
            translator.translate("passo1 passo2", false)
        } returns "step1 step2"

        coEvery { repository.create(any(), any(), any()) } returns 1

        val result = service.create(cocktail)

        coVerify {
            translator.translate("passo1 passo2", false)
        }

        coVerify {
            repository.create(
                cocktail,
                "step1 step2",
                "passo1 passo2"
            )
        }

        assertEquals(1, result)
    }

    @Test
    fun `create translates italian instructions when missing`() = runTest {
        val cocktail = mockCocktail(
            instructions = listOf("step1", "step2"),
            instructionsIt = emptyList()
        )

        coEvery {
            translator.translate("step1 step2", true)
        } returns "passo1 passo2"

        coEvery { repository.create(any(), any(), any()) } returns 1

        val result = service.create(cocktail)

        coVerify {
            translator.translate("step1 step2", true)
        }

        coVerify {
            repository.create(
                cocktail,
                "step1 step2",
                "passo1 passo2"
            )
        }

        assertEquals(1, result)
    }

    @Test
    fun `updateVisualizations delegates to repository`() = runTest {
        coEvery { repository.updateVisualizations(1) } returns 1

        val result = service.updateVisualizations(1)

        coVerify { repository.updateVisualizations(1) }
        assertEquals(1, result)
    }

    @Test
    fun `readSingle delegates to repository`() = runTest {
        val cocktail = mockk<ExposedCocktail>()

        coEvery { repository.readSingle(1) } returns cocktail

        val result = service.readSingle(1)

        coVerify { repository.readSingle(1) }
        assertEquals(cocktail, result)
    }

    @Test
    fun `readAll delegates to repository`() = runTest {
        val list = mockk<ExposedCocktailList>()

        coEvery { repository.readAll() } returns list

        val result = service.readAll()

        coVerify { repository.readAll() }
        assertEquals(list, result)
    }

    @Test
    fun `readMostPopular delegates to repository`() = runTest {
        val list = mockk<ExposedCocktailList>()

        coEvery { repository.readMostPopular(5) } returns list

        val result = service.readMostPopular(5)

        coVerify { repository.readMostPopular(5) }
        assertEquals(list, result)
    }

    // helper
    private fun mockCocktail(
        instructions: List<String>,
        instructionsIt: List<String>
    ): ExposedCocktail {
        return ExposedCocktail(
            id = "1",
            name = "name",
            category = "cat",
            instructions = instructions.map {
                ExposedCocktailInstruction("instruction", it)
            },
            instructionsIt = instructionsIt.map {
                ExposedCocktailInstruction("instruction", it)
            },
            glass = "glass",
            isAlcoholic = true,
            imageLink = "",
            videoLink = "",
            type = "type",
            method = "method",
            ingredients = mockk(),
            visualizations = 0,
            tags = ExposedCocktailTags(emptyList()),
            userId = "user",
            username = "username"
        )
    }
}