package com.zioanacleto.cocktails.service

import com.zioanacleto.cocktails.ExposedCocktail
import com.zioanacleto.cocktails.InstructionsTranslator
import com.zioanacleto.cocktails.repository.CocktailsRepository
import org.slf4j.LoggerFactory

class CocktailsService(
    private val repository: CocktailsRepository,
    private val instructionsTranslator: InstructionsTranslator
) {
    private val log = LoggerFactory.getLogger(CocktailsService::class.java)

    suspend fun create(cocktail: ExposedCocktail): Int {
        val cocktailInstructions =
            cocktail.instructions.joinToString(" ") { it.instruction }.ifEmpty {
                instructionsTranslator
                    .translate(
                        text = cocktail.instructionsIt.joinToString(" ") { it.instruction },
                        isFromEnglish = false
                    )
            }
        log.debug("Create function, english instructions: $cocktailInstructions")

        val cocktailInstructionsIt =
            cocktail.instructionsIt.joinToString(" ") { it.instruction }.ifEmpty {
                instructionsTranslator
                    .translate(
                        text = cocktail.instructions.joinToString(" ") { it.instruction },
                    )
            }
        log.debug("Create function, italian instructions: $cocktailInstructionsIt")

        return repository.create(cocktail, cocktailInstructions, cocktailInstructionsIt)
    }

    suspend fun updateVisualizations(cocktailId: Int) = repository.updateVisualizations(cocktailId)

    suspend fun readSingle(id: Int) = repository.readSingle(id)

    suspend fun readSingleWithName(name: String) = repository.readSingleWithName(name)

    suspend fun readAllWithCategory(category: String) = repository.readAllWithCategory(category)

    suspend fun readAllWithType(type: String) = repository.readAllWithType(type)

    suspend fun readAllWithIngredient(ingredientName: String) = repository.readAllWithIngredient(ingredientName)

    suspend fun readAll() = repository.readAll()

    suspend fun readMostPopular(size: Int) = repository.readMostPopular(size)
}