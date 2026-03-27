package com.zioanacleto.cocktails.service

import com.zioanacleto.cocktails.ExposedCocktail
import com.zioanacleto.cocktails.InstructionsTranslator
import com.zioanacleto.cocktails.repository.CocktailsRepository
import org.slf4j.LoggerFactory

class CocktailsServiceImpl(
    private val repository: CocktailsRepository,
    private val instructionsTranslator: InstructionsTranslator
) : CocktailsService {
    private val log = LoggerFactory.getLogger(CocktailsServiceImpl::class.java)

    override suspend fun create(cocktail: ExposedCocktail): Int {
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

    override suspend fun updateVisualizations(cocktailId: Int) = repository.updateVisualizations(cocktailId)

    override suspend fun readSingle(id: Int) = repository.readSingle(id)

    override suspend fun readSingleWithName(name: String) = repository.readSingleWithName(name)

    override suspend fun readAllWithCategory(category: String) = repository.readAllWithCategory(category)

    override suspend fun readAllWithType(type: String) = repository.readAllWithType(type)

    override suspend fun readAllWithIngredient(ingredientName: String) =
        repository.readAllWithIngredient(ingredientName)

    override suspend fun readAll() = repository.readAll()

    override suspend fun readMostPopular(size: Int) = repository.readMostPopular(size)
}