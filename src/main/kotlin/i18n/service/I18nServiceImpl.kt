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

        val existingKeys = repository.getAllTextIds().toSet()
        val newStrings = request.strings.filter { it.key !in existingKeys }

        log.debug("New strings size: {}", newStrings.size)

        // 1️⃣ Insert new keys
        newStrings.forEach {
            repository.insertNewString(it.key)
        }

        // 2️⃣ Insert base translations
        request.strings.forEach { input ->
            val exists = repository.translationExists(input.key, input.language)

            if (!exists) {
                repository.insertNewTranslation(
                    keyTextId = input.key,
                    translation = input.value,
                    translationLanguage = input.language,
                    currentDate = now
                )
                counter++
            }
        }

        // 3️⃣ AUTO TRANSLATION (BATCH)
        val baseLanguage = "en"
        val targetLanguage = "it"

        val baseStrings = request.strings.filter { it.language == baseLanguage }

        // Filtra solo quelli che NON hanno già traduzione
        val toTranslate = baseStrings.filterNot {
            repository.translationExists(it.key, targetLanguage)
        }

        log.debug("Strings to translate: {}", toTranslate.size)

        // ⚡ Chunk per evitare limiti HuggingFace
        val chunkSize = 20

        toTranslate.chunked(chunkSize).forEach { chunk ->
            val texts = chunk.map { it.value }

            val translatedTexts = try {
                translator.translateMultipleTexts(texts)
            } catch (e: Exception) {
                log.error("Batch translation failed, fallback to single", e)

                // 🔥 fallback intelligente
                texts.map { text ->
                    try {
                        translator.translateSingleText(text, true)
                    } catch (e: Exception) {
                        text
                    }
                }
            }

            // Safety check (importantissimo)
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
                counter++
            }
        }

        return counter
    }
}