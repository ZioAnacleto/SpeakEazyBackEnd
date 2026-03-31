package i18n.service

import com.zioanacleto.i18n.ExposedI18nRequest
import com.zioanacleto.i18n.I18nKeyAndValue
import com.zioanacleto.i18n.repository.I18nRepository
import com.zioanacleto.i18n.service.I18nServiceImpl
import com.zioanacleto.i18n.translator.Translator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class I18nServiceImplTest {

    private lateinit var repository: I18nRepository
    private lateinit var translator: Translator
    private lateinit var service: I18nServiceImpl

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        translator = mockk(relaxed = true)
        service = I18nServiceImpl(repository, translator)
    }

    // -----------------------------
    // BASE INSERT
    // -----------------------------

    @Test
    fun `should insert new keys and base translations`() = runTest {
        val request = ExposedI18nRequest(
            app = "testApp",
            strings = listOf(
                I18nKeyAndValue("home", "Home", "en")
            )
        )

        coEvery { repository.getAllTextIds() } returns emptyList()
        coEvery { repository.translationExists(any(), any()) } returns false
        coEvery { translator.translateMultipleTexts(any()) } returns listOf("Casa")

        val result = service.insertStrings(request)

        coVerify { repository.insertNewString("home") }
        coVerify {
            repository.insertNewTranslation("home", "Home", "en", any())
        }
        coVerify {
            repository.insertNewTranslation("home", "Casa", "it", any())
        }

        assertEquals(2, result)
    }

    // -----------------------------
    // NO DUPLICATE KEYS
    // -----------------------------

    @Test
    fun `should not insert existing keys`() = runTest {
        val request = ExposedI18nRequest(
            app = "testApp",
            strings = listOf(
                I18nKeyAndValue("home", "Home", "en")
            )
        )

        coEvery { repository.getAllTextIds() } returns listOf("home")
        coEvery { repository.translationExists(any(), any()) } returns false
        coEvery { translator.translateMultipleTexts(any()) } returns listOf("Casa")

        service.insertStrings(request)

        coVerify(exactly = 0) { repository.insertNewString(any()) }
    }

    // -----------------------------
    // SKIP EXISTING TRANSLATION
    // -----------------------------

    @Test
    fun `should skip existing translations`() = runTest {
        val request = ExposedI18nRequest(
            app = "testApp",
            strings = listOf(
                I18nKeyAndValue("home", "Home", "en")
            )
        )

        coEvery { repository.getAllTextIds() } returns emptyList()

        coEvery { repository.translationExists("home", "en") } returns true
        coEvery { repository.translationExists("home", "it") } returns true

        val result = service.insertStrings(request)

        coVerify(exactly = 0) {
            repository.insertNewTranslation(any(), any(), any(), any())
        }

        assertEquals(0, result)
    }

    // -----------------------------
    // BATCH TRANSLATION
    // -----------------------------

    @Test
    fun `should translate using batch`() = runTest {
        val request = ExposedI18nRequest(
            app = "testApp",
            strings = listOf(
                I18nKeyAndValue("home", "Home", "en"),
                I18nKeyAndValue("login", "Login", "en")
            )
        )

        coEvery { repository.getAllTextIds() } returns emptyList()
        coEvery { repository.translationExists(any(), any()) } returns false

        coEvery {
            translator.translateMultipleTexts(listOf("Home", "Login"))
        } returns listOf("Casa", "Accesso")

        val result = service.insertStrings(request)

        assertEquals(4, result)

        coVerify {
            repository.insertNewTranslation("home", "Casa", "it", any())
        }
        coVerify {
            repository.insertNewTranslation("login", "Accesso", "it", any())
        }
    }

    // -----------------------------
    // FALLBACK TO SINGLE
    // -----------------------------

    @Test
    fun `should fallback to single translation if batch fails`() = runTest {
        val request = ExposedI18nRequest(
            app = "testApp",
            strings = listOf(
                I18nKeyAndValue("home", "Home", "en")
            )
        )

        coEvery { repository.getAllTextIds() } returns emptyList()
        coEvery { repository.translationExists(any(), any()) } returns false

        coEvery {
            translator.translateMultipleTexts(any())
        } throws RuntimeException("Batch error")

        coEvery {
            translator.translateSingleText("Home", true)
        } returns "Casa"

        val result = service.insertStrings(request)

        coVerify {
            translator.translateSingleText("Home", true)
        }

        coVerify {
            repository.insertNewTranslation("home", "Casa", "it", any())
        }

        assertEquals(2, result)
    }

    // -----------------------------
    // FALLBACK FAILURE (use original)
    // -----------------------------

    @Test
    fun `should fallback to original text if single translation fails`() = runTest {
        val request = ExposedI18nRequest(
            app = "testApp",
            strings = listOf(
                I18nKeyAndValue("home", "Home", "en")
            )
        )

        coEvery { repository.getAllTextIds() } returns emptyList()
        coEvery { repository.translationExists(any(), any()) } returns false

        coEvery { translator.translateMultipleTexts(any()) } throws RuntimeException()
        coEvery { translator.translateSingleText(any(), any()) } throws RuntimeException()

        service.insertStrings(request)

        coVerify {
            repository.insertNewTranslation("home", "Home", "it", any())
        }
    }

    // -----------------------------
    // CHUNKING
    // -----------------------------

    @Test
    fun `should split into chunks when exceeding chunk size`() = runTest {
        val strings = (1..25).map {
            I18nKeyAndValue("key$it", "value$it", "en")
        }

        val request = ExposedI18nRequest(app = "testApp", strings)

        coEvery { repository.getAllTextIds() } returns emptyList()
        coEvery { repository.translationExists(any(), any()) } returns false

        coEvery { translator.translateMultipleTexts(any()) } answers {
            firstArg<List<String>>().map { "it_$it" }
        }

        val result = service.insertStrings(request)

        // 25 en + 25 it
        assertEquals(50, result)

        // Verifica che batch sia stato chiamato più volte (chunking)
        coVerify(atLeast = 2) {
            translator.translateMultipleTexts(any())
        }
    }

    // -----------------------------
    // SIZE MISMATCH SAFETY
    // -----------------------------

    @Test
    fun `should skip chunk if translation size mismatch`() = runTest {
        val request = ExposedI18nRequest(
            app = "testApp",
            strings = listOf(
                I18nKeyAndValue("home", "Home", "en"),
                I18nKeyAndValue("login", "Login", "en")
            )
        )

        coEvery { repository.getAllTextIds() } returns emptyList()
        coEvery { repository.translationExists(any(), any()) } returns false

        coEvery {
            translator.translateMultipleTexts(any())
        } returns listOf("Casa") // mismatch size

        val result = service.insertStrings(request)

        // solo base insert (2)
        assertEquals(2, result)

        coVerify(exactly = 0) {
            repository.insertNewTranslation(any(), "Casa", "it", any())
        }
    }
}