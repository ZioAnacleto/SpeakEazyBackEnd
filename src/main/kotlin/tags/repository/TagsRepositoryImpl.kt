package com.zioanacleto.tags.repository

import com.zioanacleto.dbQuery
import com.zioanacleto.tags.ExposedTag
import com.zioanacleto.tags.ExposedTagList
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class TagsRepositoryImpl(database: Database) : TagsRepository {
    object Tags : Table() {
        val id = integer(DB_KEY_ID).autoIncrement()
        val name = varchar(DB_KEY_NAME, length = 100)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Tags)
        }
    }

    override suspend fun readAll(): ExposedTagList =
        dbQuery {
            ExposedTagList(
                Tags.selectAll()
                    .mapNotNull {
                        ExposedTag(
                            id = it[Tags.id].toString(),
                            name = it[Tags.name]
                        )
                    }
            )
        }

    companion object {
        private const val DB_KEY_ID = "id"
        private const val DB_KEY_NAME = "name"
    }
}