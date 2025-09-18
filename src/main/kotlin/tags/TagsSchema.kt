package com.zioanacleto.tags

import com.zioanacleto.dbQuery
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class TagsService(database: Database) {
    object Tags : Table() {
        val id = integer(DB_KEY_ID).autoIncrement()
        val name = varchar(DB_KEY_NAME, length = 100)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Tags)
        }
    }

    suspend fun readAll(): ExposedTagList {
        return dbQuery {
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
    }

    companion object {
        private const val DB_KEY_ID = "id"
        private const val DB_KEY_NAME = "name"
    }
}