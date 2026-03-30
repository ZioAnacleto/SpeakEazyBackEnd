package i18n.service

import com.zioanacleto.cocktails.InstructionsTranslator
import com.zioanacleto.i18n.ExposedI18nRequest
import com.zioanacleto.i18n.I18nKeyAndValue
import com.zioanacleto.i18n.repository.I18nRepository
import com.zioanacleto.i18n.service.I18nServiceImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class I18nServiceImplTest {

    private lateinit var repository: I18nRepository
    private lateinit var translator: InstructionsTranslator
    private lateinit var service: I18nServiceImpl

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        translator = mockk(relaxed = true)
        service = I18nServiceImpl(repository, translator)
    }

    // -----------------------------
    // BASIC INSERT
    // -----------------------------

    @Test
    fun `should insert new keys and base translations`() = runTest {
        val request = ExposedI18nRequest(
            app = "testApp",
            strings = listOf(
                I18nKeyAndValue("home_title", "Home", "en")
            )
        )

        coEvery { repository.getAllTextIds() } returns emptyList()
        coEvery { repository.translationExists(any(), any()) } returns false

        val result = service.insertStrings(request)

        coVerify { repository.insertNewString("home_title") }
        coVerify {
            repository.insertNewTranslation(
                "home_title", "Home", "en", any()
            )
        }

        assertEquals(2, result) // en + auto it
    }

    // -----------------------------
    // NO DUPLICATES
    // -----------------------------

    @Test
    fun `should not insert existing keys`() = runTest {
        val request = ExposedI18nRequest(
            app = "testApp",
            strings = listOf(
                I18nKeyAndValue("home_title", "Home", "en")
            )
        )

        coEvery { repository.getAllTextIds() } returns listOf("home_title")
        coEvery { repository.translationExists(any(), any()) } returns false

        service.insertStrings(request)

        coVerify(exactly = 0) { repository.insertNewString(any()) }
    }

    // -----------------------------
    // TRANSLATION EXISTS
    // -----------------------------

    @Test
    fun `should not insert translation if already exists`() = runTest {
        val request = ExposedI18nRequest(
            app = "testApp",
            strings = listOf(
                I18nKeyAndValue("home_title", "Home", "en")
            )
        )

        coEvery { repository.getAllTextIds() } returns emptyList()

        coEvery {
            repository.translationExists("home_title", "en")
        } returns true

        coEvery {
            repository.translationExists("home_title", "it")
        } returns true

        val result = service.insertStrings(request)

        coVerify(exactly = 0) {
            repository.insertNewTranslation(any(), any(), any(), any())
        }

        assertEquals(0, result)
    }

    // -----------------------------
    // AUTO TRANSLATION
    // -----------------------------

    @Test
    fun `should auto translate from en to it`() = runTest {
        val request = ExposedI18nRequest(
            app = "testApp",
            strings = listOf(
                I18nKeyAndValue("home_title", "Home", "en")
            )
        )

        coEvery { repository.getAllTextIds() } returns emptyList()

        coEvery {
            repository.translationExists("home_title", "en")
        } returns false

        coEvery {
            repository.translationExists("home_title", "it")
        } returns false

        coEvery {
            translator.translate("Home", true)
        } returns "Casa"

        val result = service.insertStrings(request)

        coVerify {
            repository.insertNewTranslation(
                "home_title", "Casa", "it", any()
            )
        }

        assertEquals(2, result)
    }

    // -----------------------------
    // TRANSLATOR FAILURE (fallback)
    // -----------------------------

    @Test
    fun `should fallback to original text if translator fails`() = runTest {
        val request = ExposedI18nRequest(
            app = "testApp",
            strings = listOf(
                I18nKeyAndValue("home_title", "Home", "en")
            )
        )

        coEvery { repository.getAllTextIds() } returns emptyList()

        coEvery { repository.translationExists(any(), "en") } returns false
        coEvery { repository.translationExists(any(), "it") } returns false

        coEvery {
            translator.translate(any(), any())
        } throws RuntimeException("AI error")

        service.insertStrings(request)

        coVerify {
            repository.insertNewTranslation(
                "home_title", "Home", "it", any()
            )
        }
    }

    // -----------------------------
    // MULTIPLE STRINGS
    // -----------------------------

    @Test
    fun `should handle multiple strings correctly`() = runTest {
        val request = ExposedI18nRequest(
            app = "testApp",
            strings = listOf(
                I18nKeyAndValue("home", "Home", "en"),
                I18nKeyAndValue("login", "Login", "en")
            )
        )

        coEvery { repository.getAllTextIds() } returns emptyList()
        coEvery { repository.translationExists(any(), any()) } returns false
        coEvery { translator.translate(any(), any()) } answers {
            "IT_${firstArg<String>()}"
        }

        val result = service.insertStrings(request)

        // 2 en + 2 it
        assertEquals(4, result)

        coVerify(exactly = 2) { repository.insertNewString(any()) }
        coVerify(exactly = 4) {
            repository.insertNewTranslation(any(), any(), any(), any())
        }
    }
}