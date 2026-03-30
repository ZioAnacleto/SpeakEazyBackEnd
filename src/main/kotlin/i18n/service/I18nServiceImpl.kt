package com.zioanacleto.i18n.service

import com.zioanacleto.cocktails.InstructionsTranslator
import com.zioanacleto.i18n.ExposedI18nRequest
import com.zioanacleto.i18n.repository.I18nRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.time.Instant

class I18nServiceImpl(
    private val repository: I18nRepository,
    private val translator: InstructionsTranslator
): I18nService {
    override suspend fun insertStrings(request: ExposedI18nRequest): Int {
        val now = Instant.now().toString()
        var counter = 0

        val existingKeys = repository.getAllTextIds().toSet()

        val newStrings = request.strings.filter { it.key !in existingKeys }

        // insert new textIds
        newStrings.forEach { dto ->
            repository.insertNewString(dto.key)
        }

        // insert base translations from request
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

        // AUTO-TRANSLATE (EN -> IT)
        val baseLanguage = "en"
        val targetLanguage = "it"

        val baseStrings = request.strings.filter { it.language == baseLanguage }
        // Using a Semaphore to avoid rate limit for HuggingFace
        val semaphore = Semaphore(5)

        coroutineScope {
            baseStrings.map { input ->
                async {
                    semaphore.withPermit {
                        val alreadyExists = repository.translationExists(input.key, targetLanguage)

                        if (!alreadyExists) {
                            val translated = try {
                                translator.translate(input.value, true)
                            } catch (e: Exception) {
                                input.value
                            }

                            repository.insertNewTranslation(
                                keyTextId = input.key,
                                translation = translated,
                                translationLanguage = targetLanguage,
                                currentDate = now
                            )

                            1
                        } else 0
                    }
                }
            }.awaitAll().sum().also {
                counter += it
            }
        }

        return counter
    }
}