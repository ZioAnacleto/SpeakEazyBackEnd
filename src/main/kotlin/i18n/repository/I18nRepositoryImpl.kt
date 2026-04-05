package com.zioanacleto.i18n.repository

import com.zioanacleto.dbQuery
import com.zioanacleto.i18n.I18nKeyValueLanguage
import com.zioanacleto.i18n.Language
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.transactions.transaction

class I18nRepositoryImpl(
    database: Database
) : I18nRepository {
    object I18nTextIds : Table() {
        val id = integer(DB_KEY_ID).autoIncrement().uniqueIndex()
        val textId = varchar(DB_KEY_TEXT_ID, 2000)
        val isTranslated = bool(DB_KEY_IS_TRANSLATED)

        override val primaryKey: PrimaryKey = PrimaryKey(id)
    }

    object I18nTranslations : Table() {
        val id = integer(DB_KEY_ID).autoIncrement().uniqueIndex()
        val textId = varchar(DB_KEY_TEXT_ID, 2000)
        val language = varchar(DB_KEY_LANGUAGE, 10)
        val value = varchar(DB_KEY_VALUE, 2000)
        val lastUpdate = varchar(DB_KEY_LAST_UPDATE, 100)

        override val primaryKey: PrimaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(I18nTextIds)
            SchemaUtils.create(I18nTranslations)
        }
    }

    override suspend fun insertNewString(newTextId: String) = dbQuery {
        I18nTextIds.insert {
            it[textId] = newTextId
            it[isTranslated] = false
        }[I18nTextIds.id]
    }

    override suspend fun insertNewTranslation(
        keyTextId: String,
        translation: String,
        translationLanguage: String,
        currentDate: String
    ) = dbQuery {
        I18nTranslations.insert {
            it[textId] = keyTextId
            it[value] = translation
            it[language] = translationLanguage
            it[lastUpdate] = currentDate
        }[I18nTranslations.id]
    }

    override suspend fun getAllTextIds(): List<String> = dbQuery {
        I18nTextIds
            .select(I18nTextIds.textId)
            .map { it[I18nTextIds.textId] }
    }

    override suspend fun translationExists(
        key: String,
        language: String
    ): Boolean = dbQuery {
        I18nTranslations
            .select(
                (I18nTranslations.textId eq key) and
                        (I18nTranslations.language eq language)
            )
            .limit(1)
            .any()
    }

    override suspend fun getAllTranslations(): List<Pair<String, String>> = dbQuery {
        I18nTranslations
            .select(
                I18nTranslations.textId,
                I18nTranslations.language
            )
            .map { it[I18nTranslations.textId] to it[I18nTranslations.language] }
    }

    override suspend fun markAsTranslatedIfComplete(key: String) = dbQuery {
        val count = I18nTranslations
            .select(
                (I18nTranslations.textId eq key) and
                        (I18nTranslations.language inList Language.entries.map { it.code })
            )
            .count()

        if (count >= 2) {
            I18nTextIds.update({ I18nTextIds.textId eq key }) {
                it[isTranslated] = true
            }
        }
    }

    override suspend fun getAllTranslationsFull(): List<I18nKeyValueLanguage> = dbQuery {
        I18nTranslations
            .selectAll()
            .map {
                I18nKeyValueLanguage(
                    key = it[I18nTranslations.textId],
                    value = it[I18nTranslations.value],
                    language = it[I18nTranslations.language]
                )
            }
    }

    override suspend fun getTranslationValue(
        key: String,
        language: String
    ): String? = dbQuery {
        I18nTranslations
            .select(
                (I18nTranslations.textId eq key) and
                        (I18nTranslations.language eq language)
            )
            .limit(1)
            .map { it[I18nTranslations.value] }
            .firstOrNull()
    }

    override suspend fun updateTranslation(
        keyTextId: String,
        translation: String,
        translationLanguage: String,
        currentDate: String
    ): Int = dbQuery {
        I18nTranslations.update({
            (I18nTranslations.textId eq keyTextId) and
                    (I18nTranslations.language eq translationLanguage)
        }) {
            it[value] = translation
            it[lastUpdate] = currentDate
        }
    }

    override suspend fun deleteTranslationsByKeyExceptLanguage(
        key: String,
        languageToKeep: String
    ) = dbQuery {
        I18nTranslations.deleteWhere {
            (textId eq key) and (language neq languageToKeep)
        }
    }

    companion object {
        private const val DB_KEY_ID = "id"
        private const val DB_KEY_TEXT_ID = "textid"
        private const val DB_KEY_IS_TRANSLATED = "istranslated"
        private const val DB_KEY_LANGUAGE = "language"
        private const val DB_KEY_VALUE = "value"
        private const val DB_KEY_LAST_UPDATE = "lastupdate"
    }
}