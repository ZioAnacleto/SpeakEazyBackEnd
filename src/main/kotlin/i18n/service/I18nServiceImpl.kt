package com.zioanacleto.i18n.service

import com.zioanacleto.i18n.ExposedI18nRequest
import com.zioanacleto.i18n.repository.I18nRepository
import com.zioanacleto.i18n.translator.Translator
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.slf4j.LoggerFactory
import java.time.Instant

class I18nServiceImpl(
    private val repository: I18nRepository,
    private val translator: Translator
) : I18nService {

    private val log = LoggerFactory.getLogger(I18nServiceImpl::class.java)

    override suspend fun insertStrings(request: ExposedI18nRequest): Int {
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

        log.debug("New strings inserted in DB.")

        val existingTranslations = repository.getAllTranslations().toMutableSet()

        // Insert base translations
        request.strings.forEach { input ->
            val exists = (input.key to input.language) in existingTranslations

            if (!exists) {
                repository.insertNewTranslation(
                    keyTextId = input.key,
                    translation = input.value,
                    translationLanguage = input.language,
                    currentDate = now
                )
                existingTranslations.add(input.key to input.language)
                counter++
            }
        }

        // AI auto translation via batch
        val baseLanguage = "en"
        val targetLanguage = "it"

        val baseStrings = request.strings.filter { it.language == baseLanguage }

        // Filter out the already translated textIds
        val toTranslate = baseStrings.filterNot {
            (it.key to targetLanguage) in existingTranslations
        }

        log.debug("Strings to translate: {}", toTranslate.size)

        // Chunk to avoid API limits
        val chunkSize = 20

        toTranslate.chunked(chunkSize).forEach { chunk ->
            val texts = chunk.map { it.value }

            val translatedTexts = try {
                translator.translateMultipleTexts(texts)
            } catch (e: Exception) {
                log.error("Batch translation failed, fallback to single", e)

                // fallback
                texts.map { text ->
                    try {
                        translator.translateSingleText(text, true)
                    } catch (e: Exception) {
                        text
                    }
                }
            }

            // Safety check
            if (translatedTexts.size != chunk.size) {
                log.error("Mismatch translations size. Fallback chunk.")
                return@forEach
            }

            chunk.zip(translatedTexts).forEach { (input, translated) ->
                repository.insertNewTranslation(
                    keyTextId = input.key,
                    translation = translated,
                    translationLanguage = targetLanguage,
                    currentDate = now
                )
                existingTranslations.add(input.key to targetLanguage)
                counter++
            }
        }

        return counter
    }
}