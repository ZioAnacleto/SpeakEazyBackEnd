package i18n.repository

import com.zioanacleto.i18n.repository.I18nRepository
import com.zioanacleto.i18n.repository.I18nRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.testcontainers.containers.PostgreSQLContainer

class I18nRepositoryImplTest {

    private lateinit var database: Database
    private lateinit var postgres: PostgreSQLContainer<*>
    private lateinit var repository: I18nRepository

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
        repository = I18nRepositoryImpl(database)
    }

    @After
    fun tearDown() {
        postgres.stop()
    }

    @Test
    fun `insertNewString should insert and return id`() = runBlocking {
        val id = repository.insertNewString("home_title")

        assertTrue(id > 0)

        val all = repository.getAllTextIds()
        assertTrue(all.contains("home_title"))
    }

    @Test
    fun `getAllTextIds should return all inserted keys`() = runBlocking {
        repository.insertNewString("key1")
        repository.insertNewString("key2")

        val result = repository.getAllTextIds()

        assertEquals(2, result.size)
        assertTrue(result.contains("key1"))
        assertTrue(result.contains("key2"))
    }

    @Test
    fun `getAllTextIds should return empty list when no data`() = runBlocking {
        val result = repository.getAllTextIds()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `insertNewTranslation should insert correctly`() = runBlocking {
        repository.insertNewString("home_title")

        val id = repository.insertNewTranslation(
            keyTextId = "home_title",
            translation = "Home",
            translationLanguage = "en",
            currentDate = "now"
        )

        assertTrue(id > 0)

        val exists = repository.translationExists("home_title", "en")
        assertTrue(exists)
    }

    @Test
    fun `translationExists should return true if translation exists`() = runBlocking {
        repository.insertNewString("home_title")
        repository.insertNewTranslation(
            "home_title",
            "Home",
            "en",
            "now"
        )

        val exists = repository.translationExists("home_title", "en")

        assertTrue(exists)
    }

    @Test
    fun `translationExists should return false if translation does not exist`() = runBlocking {
        repository.insertNewString("home_title")

        val exists = repository.translationExists("home_title", "it")

        assertFalse(exists)
    }

    @Test
    fun `translationExists should return false if key does not exist`() = runBlocking {
        val exists = repository.translationExists("non_existing", "en")

        assertFalse(exists)
    }
}