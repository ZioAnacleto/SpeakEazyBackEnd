package com.zioanacleto.cocktails

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.LargeTextColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.api.PreparedStatementApi
import org.postgresql.util.PGobject

class JsonColumnType : ColumnType<String>() {
    override fun sqlType() = "JSONB"

    override fun valueFromDB(value: Any): String? = when (value) {
        is PGobject -> value.value
        is String -> value
        else -> error("Unsupported JSONB type: ${value::class}")
    }

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        value?.let {
            val pgObject = PGobject().apply {
                type = "jsonb"
                this.value = value as? String
            }
            stmt[index] = pgObject
        } ?: stmt.setNull(index, LargeTextColumnType())
    }
}

fun Table.jsonb(name: String): Column<String> = registerColumn(name, JsonColumnType())