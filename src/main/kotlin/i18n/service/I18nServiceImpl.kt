package com.zioanacleto.i18n.service

import com.zioanacleto.i18n.*
import com.zioanacleto.i18n.repository.I18nRepository
import com.zioanacleto.i18n.repository.I18nRepositoryImpl
import com.zioanacleto.i18n.translator.Translator
import org.slf4j.LoggerFactory
import java.time.Instant

class I18nServiceImpl(
    private val repository: I18nRepository,
    private val translator: Translator
) : I18nService {

    private val log = LoggerFactory.getLogger(I18nServiceImpl::class.java)

    override suspend fun insertBaseStrings(request: ExposedI18nRequest): Int {
        val now = Instant.now().toString()
        var counter = 0

        log.debug("Starting inserting new strings")
        val existingKeys = repository.getAllTextIds().toSet()

        log.debug("existingKeys: {}, size: {}", existingKeys, existingKeys.size)
        val newStrings = request.strings.filter { it.key !in existingKeys }

        log.debug("New strings size: {}", newStrings.size)

        // Insert new keys
        newStrings.forEach {
            repository.insertNewString(it.key)
        }

        request.strings.forEach { input ->
            val existingValue = repository.getTranslationValue(input.key, input.language)

            existingValue?.let {
                if (it != input.value) {
                    repository.updateTranslation(
                        keyTextId = input.key,
                        translation = input.value,
                        translationLanguage = input.language,
                        currentDate = now
                    )

                    repository.deleteTranslationsByKeyExceptLanguage(
                        key = input.key,
                        languageToKeep = Language.ENGLISH.code
                    )

                    counter++
                }
            } ?: run {
                // nuovo → insert
                repository.insertNewTranslation(
                    keyTextId = input.key,
                    translation = input.value,
                    translationLanguage = input.language,
                    currentDate = now
                )
                counter++
            }
        }

        return counter
    }

    override suspend fun generateTranslationsAsync(request: ExposedI18nRequest) {
        val now = Instant.now().toString()
        val existingTranslations = repository.getAllTranslations().toMutableSet()

        // Filter out the already translated textIds
        val baseStrings = request.strings.filter { it.language == Language.ENGLISH.code }
        val toTranslate = baseStrings.filterNot {
            (it.key to Language.ITALIAN.code) in existingTranslations
        }

        log.debug("Strings to translate: {}", toTranslate.size)

        // Chunk to avoid API limits
        val chunkSize = 20

        toTranslate.chunked(chunkSize).forEach { chunk ->
            val texts = chunk.map { it.value }

            val translatedTexts = try {
                translator.translateMultipleTexts(texts)
            } catch (e: Exception) {
                texts.map { translator.translateSingleText(it, true) }
            }

            chunk.zip(translatedTexts).forEach { (input, translated) ->
                repository.insertNewTranslation(
                    keyTextId = input.key,
                    translation = translated,
                    translationLanguage = Language.ITALIAN.code,
                    currentDate = now
                )

                existingTranslations.add(input.key to Language.ITALIAN.code)

                // update status
                repository.markAsTranslatedIfComplete(input.key)
            }
        }
    }

    override suspend fun exportTranslations(): ExposedI18nResponse =
        repository.getAllTranslationsFull()
            .groupBy { it.language }
            .map { (language, items) ->
                ExposedI18nResponseLanguages(
                    language = language,
                    strings = items.map {
                        I18nKeyValue(it.key, it.value)
                    }
                )
            }
            .let { languages ->
                ExposedI18nResponse(
                    app = "speakeazy-android",
                    languages = languages
                )
            }

    override suspend fun hasUpdates(): I18nUpdates {
        val latestDbUpdate = repository.getLatestUpdate()
        val lastPublished = repository.getMetadata(I18nRepositoryImpl.LAST_PUBLISHED_DATE)
        val lastVersion = repository.getMetadata(I18nRepositoryImpl.LAST_PUBLISHED_VERSION)

        val hasUpdates = latestDbUpdate != null && latestDbUpdate != lastPublished

        return I18nUpdates(
            hasUpdates = hasUpdates,
            version = lastVersion ?: ""
        )
    }

    override suspend fun getLatestUpdate(): String? = repository.getLatestUpdate()

    override suspend fun uploadMetadata(key: String, value: String) {
        repository.setMetadata(key, value)
    }
}