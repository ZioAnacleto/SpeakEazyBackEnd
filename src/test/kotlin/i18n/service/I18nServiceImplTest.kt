package i18n.service

import com.zioanacleto.i18n.ExposedI18nRequest
import com.zioanacleto.i18n.I18nKeyValueLanguage
import com.zioanacleto.i18n.Language
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
                I18nKeyValueLanguage("home", "Home", "en")
            )
        )

        coEvery { repository.getAllTextIds() } returns emptyList()
        coEvery { repository.getTranslationValue("home", "en") } returns null

        val result = service.insertBaseStrings(request)

        coVerify { repository.insertNewString("home") }
        coVerify {
            repository.insertNewTranslation("home", "Home", "en", any())
        }

        assertEquals(1, result)
    }

    @Test
    fun `should not insert existing keys`() = runTest {
        val request = ExposedI18nRequest(
            app = "testApp",
            strings = listOf(
                I18nKeyValueLanguage("home", "Home", "en")
            )
        )

        coEvery { repository.getAllTextIds() } returns listOf("home")
        coEvery { repository.getAllTranslations() } returns emptyList()

        service.insertBaseStrings(request)

        coVerify(exactly = 0) { repository.insertNewString(any()) }
    }

    @Test
    fun `should skip existing base translation if value is unchanged`() = runTest {
        val request = ExposedI18nRequest(
            app = "testApp",
            strings = listOf(
                I18nKeyValueLanguage("home", "Home", "en")
            )
        )

        coEvery { repository.getAllTextIds() } returns emptyList()
        coEvery { repository.getTranslationValue("home", "en") } returns "Home"

        val result = service.insertBaseStrings(request)

        coVerify(exactly = 0) {
            repository.insertNewTranslation(any(), any(), any(), any())
        }
        coVerify(exactly = 0) {
            repository.updateTranslation(any(), any(), any(), any())
        }

        assertEquals(0, result)
    }

    @Test
    fun `should update translation and invalidate others if value changed`() = runTest {
        val request = ExposedI18nRequest(
            app = "testApp",
            strings = listOf(
                I18nKeyValueLanguage("home", "Sign in", "en")
            )
        )

        coEvery { repository.getAllTextIds() } returns emptyList()
        coEvery { repository.getTranslationValue("home", "en") } returns "Login"

        val result = service.insertBaseStrings(request)

        coVerify {
            repository.updateTranslation("home", "Sign in", "en", any())
        }

        coVerify {
            repository.deleteTranslationsByKeyExceptLanguage(
                key = "home",
                languageToKeep = Language.ENGLISH.code
            )
        }

        coVerify(exactly = 0) {
            repository.insertNewTranslation(any(), any(), any(), any())
        }

        assertEquals(1, result)
    }

    @Test
    fun `should not insert key if already exists`() = runTest {
        val request = ExposedI18nRequest(
            app = "testApp",
            strings = listOf(
                I18nKeyValueLanguage("home", "Home", "en")
            )
        )

        coEvery { repository.getAllTextIds() } returns listOf("home")
        coEvery { repository.getTranslationValue("home", "en") } returns null

        service.insertBaseStrings(request)

        coVerify(exactly = 0) {
            repository.insertNewString(any())
        }
    }

    @Test
    fun `should handle mix of new unchanged and updated strings`() = runTest {
        val request = ExposedI18nRequest(
            app = "testApp",
            strings = listOf(
                I18nKeyValueLanguage("home", "Home", "en"),        // unchanged
                I18nKeyValueLanguage("login", "Sign in", "en"),    // updated
                I18nKeyValueLanguage("profile", "Profile", "en")   // new
            )
        )

        coEvery { repository.getAllTextIds() } returns listOf("home", "login")

        coEvery { repository.getTranslationValue("home", "en") } returns "Home"
        coEvery { repository.getTranslationValue("login", "en") } returns "Login"
        coEvery { repository.getTranslationValue("profile", "en") } returns null

        val result = service.insertBaseStrings(request)

        // new key
        coVerify { repository.insertNewString("profile") }

        // unchanged → nothing
        coVerify(exactly = 0) {
            repository.updateTranslation("home", any(), any(), any())
        }

        // updated
        coVerify {
            repository.updateTranslation("login", "Sign in", "en", any())
        }

        coVerify {
            repository.deleteTranslationsByKeyExceptLanguage(
                key = "login",
                languageToKeep = Language.ENGLISH.code
            )
        }

        // new translation
        coVerify {
            repository.insertNewTranslation("profile", "Profile", "en", any())
        }

        assertEquals(2, result) // update + insert
    }

    // -----------------------------
    // BATCH TRANSLATION
    // -----------------------------

    @Test
    fun `should translate using batch`() = runTest {
        val request = ExposedI18nRequest(
            app = "testApp",
            strings = listOf(
                I18nKeyValueLanguage("home", "Home", "en"),
                I18nKeyValueLanguage("login", "Login", "en")
            )
        )

        coEvery { repository.getAllTranslations() } returns emptyList()

        coEvery {
            translator.translateMultipleTexts(listOf("Home", "Login"))
        } returns listOf("Casa", "Accesso")

        service.generateTranslationsAsync(request)

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
                I18nKeyValueLanguage("home", "Home", "en")
            )
        )

        coEvery { repository.getAllTranslations() } returns emptyList()
        coEvery { translator.translateMultipleTexts(any()) } throws RuntimeException()
        coEvery { translator.translateSingleText("Home", true) } returns "Casa"

        service.generateTranslationsAsync(request)

        coVerify {
            translator.translateSingleText("Home", true)
        }
        coVerify {
            repository.insertNewTranslation("home", "Casa", "it", any())
        }
    }

    // -----------------------------
    // CHUNKING
    // -----------------------------

    @Test
    fun `should chunk batch requests`() = runTest {
        val strings = (1..25).map {
            I18nKeyValueLanguage("key$it", "value$it", "en")
        }
        val request = ExposedI18nRequest("testApp", strings)

        coEvery { repository.getAllTranslations() } returns emptyList()
        coEvery { translator.translateMultipleTexts(any()) } answers {
            firstArg<List<String>>().map { "it_$it" }
        }

        service.generateTranslationsAsync(request)

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
                I18nKeyValueLanguage("home", "Home", "en"),
                I18nKeyValueLanguage("login", "Login", "en")
            )
        )

        coEvery { repository.getAllTextIds() } returns emptyList()
        coEvery { repository.translationExists(any(), any()) } returns false

        coEvery {
            translator.translateMultipleTexts(any())
        } returns listOf("Casa") // mismatch size

        val result = service.insertBaseStrings(request)

        // solo base insert (2)
        assertEquals(2, result)

        coVerify(exactly = 0) {
            repository.insertNewTranslation(any(), "Casa", "it", any())
        }
    }

    @Test
    fun `should mark as translated when both languages exist`() = runTest {
        val request = ExposedI18nRequest(
            app = "testApp",
            strings = listOf(
                I18nKeyValueLanguage("home", "Home", "en")
            )
        )

        coEvery { repository.getAllTranslations() } returns listOf("home" to "en")
        coEvery { translator.translateMultipleTexts(any()) } returns listOf("Casa")

        service.generateTranslationsAsync(request)

        coVerify {
            repository.markAsTranslatedIfComplete("home")
        }
    }

    @Test
    fun `should export all translations grouped by language`() = runTest {
        coEvery { repository.getAllTranslationsFull() } returns listOf(
            I18nKeyValueLanguage(
                "home",
                "Home",
                "en"
            ),
            I18nKeyValueLanguage(
                "login",
                "Login",
                "en"
            ),
            I18nKeyValueLanguage(
                "home",
                "Casa",
                "it"
            ),
            I18nKeyValueLanguage(
                "login",
                "Accesso",
                "it"
            )
        )

        val export = service.exportTranslations()

        assertEquals(export.languages.size, 2)
        assertEquals(export.app, "speakeazy-android")
    }
}