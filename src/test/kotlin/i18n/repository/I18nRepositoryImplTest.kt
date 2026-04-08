package i18n.repository

import com.zioanacleto.i18n.repository.I18nRepository
import com.zioanacleto.i18n.repository.I18nRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
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

    @Test
    fun `getAllTranslationsFull should return empty list when no data`() = runBlocking {
        val result = repository.getAllTranslationsFull()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getAllTranslationsFull should return all inserted translations`() = runBlocking {
        repository.insertNewTranslation(
            "key1",
            "translation1",
            "it",
            "current_date"
        )
        repository.insertNewTranslation(
            "key2",
            "translation2",
            "it",
            "current_date"
        )

        val result = repository.getAllTranslationsFull()

        assertEquals(2, result.size)
        assertTrue(result[0].key == "key1")
        assertTrue(result[1].key == "key2")
    }

    @Test
    fun `getAllTranslations should return key-language pairs`() = runBlocking {
        repository.insertNewTranslation("key1", "v1", "en", "now")
        repository.insertNewTranslation("key2", "v2", "it", "now")

        val result = repository.getAllTranslations()

        assertEquals(2, result.size)
        assertTrue(result.contains("key1" to "en"))
        assertTrue(result.contains("key2" to "it"))
    }

    @Test
    fun `markAsTranslatedIfComplete should mark as translated when enough translations`() = runBlocking {
        val key = "testMarkAsTranslated"
        repository.insertNewString(key)

        repository.insertNewTranslation(key, "Home", "en", "now")
        repository.insertNewTranslation(key, "Casa", "it", "now")

        repository.markAsTranslatedIfComplete(key)

        val isTranslated = transaction(database) {
            I18nRepositoryImpl.I18nTextIds
                .selectAll()
                .where { I18nRepositoryImpl.I18nTextIds.textId eq key }
                .first()[I18nRepositoryImpl.I18nTextIds.isTranslated]
        }

        assertTrue(isTranslated)
    }

    @Test
    fun `getTranslationValue should return correct value`() = runBlocking {
        repository.insertNewTranslation(
            "testGetTranslationValue",
            "Hello",
            "en",
            "now"
        )

        val result = repository.getTranslationValue("testGetTranslationValue", "en")

        assertEquals("Hello", result)
    }

    @Test
    fun `getTranslationValue should return null if not found`() = runBlocking {
        val result = repository.getTranslationValue("missing", "en")

        assertNull(result)
    }

    @Test
    fun `updateTranslation should update value and return affected rows`() = runBlocking {
        repository.insertNewTranslation("key1", "Hello", "en", "old")

        val updated = repository.updateTranslation("key1", "Hi", "en", "new")

        assertEquals(1, updated)

        val value = repository.getTranslationValue("key1", "en")
        assertEquals("Hi", value)
    }

    @Test
    fun `deleteTranslationsByKeyExceptLanguage should delete all except specified language`() = runBlocking {
        val key = "testDeleteTranslations"

        repository.insertNewString(key)
        repository.insertNewTranslation(key, "Hello", "en", "now")
        repository.insertNewTranslation(key, "Ciao", "it", "now")

        repository.deleteTranslationsByKeyExceptLanguage(key, "en")

        val enExists = repository.translationExists(key, "en")
        val itExists = repository.translationExists(key, "it")

        assertTrue(enExists)
        assertFalse(itExists)
    }

    @Test
    fun `getLatestUpdate should return latest date`() = runBlocking {
        repository.insertNewTranslation("key1", "v1", "en", "2023")
        repository.insertNewTranslation("key2", "v2", "en", "2024")

        val result = repository.getLatestUpdate()

        assertEquals("2024", result)
    }

    @Test
    fun `getLatestUpdate should return null if no data`() = runBlocking {
        val result = repository.getLatestUpdate()

        assertNull(result)
    }

    @Test
    fun `setMetadata and getMetadata should work correctly`() = runBlocking {
        repository.setMetadata("version", "1")

        val result = repository.getMetadata("version")

        assertEquals("1", result)
    }

    @Test
    fun `setMetadata should update existing value`() = runBlocking {
        repository.setMetadata("version", "1")
        repository.setMetadata("version", "2")

        val result = repository.getMetadata("version")

        assertEquals("2", result)
    }

    @Test
    fun `getMetadata should return null if not found`() = runBlocking {
        val result = repository.getMetadata("missing")

        assertNull(result)
    }
}