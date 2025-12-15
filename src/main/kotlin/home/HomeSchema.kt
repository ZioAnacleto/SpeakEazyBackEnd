package com.zioanacleto.home

import com.zioanacleto.cocktails.CocktailService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jetbrains.exposed.sql.Database

class HomeService(private val database: Database) {

    suspend fun homeSections(): ExposedHomeSectionsList = coroutineScope {
        val config = HomeConfigManager.loadConfig()
        // take [numberOfSections] sections shuffled (including banners)
        val allSections = config.sections.shuffled().take(config.numberOfSections)
        // take only one banner (if any) from allSections
        val bannerSection = allSections.findBanner()
        val cocktailService = CocktailService(database)

        var id = 0
        // mapping all sections but banner
        val sectionDeferreds = allSections.filterOutBanner().map { section ->
            async {
                val cocktails = when (section.type.toType()) {
                    SectionType.CATEGORY -> cocktailService.readAllWithCategory(section.query!!).cocktails
                    SectionType.TYPE -> cocktailService.readAllWithType(section.query!!).cocktails
                    SectionType.POPULAR -> cocktailService.readMostPopular(section.limit!!).cocktails
                    SectionType.INGREDIENT -> cocktailService.readAllWithIngredient(section.query!!).cocktails
                    else -> emptyList()
                }
                ExposedHomeSection(
                    id = (++id).toString(),
                    name = section.title,
                    cocktails = cocktails
                )
            }
        }

        val exposedSections = sectionDeferreds.awaitAll()
        // mapping banner if available
        val banner = bannerSection?.let { section ->
            val cocktail = cocktailService.readSingleWithName(section.query!!)
            cocktail?.let { foundCocktail ->
                ExposedBanner(
                    position = section.position!!,
                    name = section.title,
                    cocktailInfo = foundCocktail,
                    cta = section.cta
                )
            }
        }

        ExposedHomeSectionsList(exposedSections, banner)
    }
}