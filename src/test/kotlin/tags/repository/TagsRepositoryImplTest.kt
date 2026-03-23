package tags.repository

import com.zioanacleto.tags.repository.TagsRepositoryImpl
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class TagsRepositoryImplTest {

    private lateinit var database: Database
    private lateinit var repository: TagsRepositoryImpl

    @Before
    fun setup() {
        database = Database.connect(
            url = "jdbc:h2:mem:test_${System.nanoTime()};DB_CLOSE_DELAY=-1;",
            driver = "org.h2.Driver"
        )

        repository = TagsRepositoryImpl(database)
    }

    @Test
    fun `readAll returns empty list when no data`() = runTest {
        val result = repository.readAll()

        assertEquals(0, result.tags.size)
    }

    @Test
    fun `readAll returns inserted tags`() = runTest {
        transaction(database) {
            TagsRepositoryImpl.Tags.insert {
                it[name] = "tag1"
            }
            TagsRepositoryImpl.Tags.insert {
                it[name] = "tag2"
            }
        }

        val result = repository.readAll()

        assertEquals(2, result.tags.size)
        assertEquals("tag1", result.tags[0].name)
        assertEquals("tag2", result.tags[1].name)
    }

    @Test
    fun `readAll maps id correctly`() = runTest {
        var insertedId: Int = -1

        transaction(database) {
            insertedId = TagsRepositoryImpl.Tags.insert {
                it[name] = "tag1"
            } get TagsRepositoryImpl.Tags.id
        }

        val result = repository.readAll()

        assertEquals(insertedId.toString(), result.tags.first().id)
    }
}