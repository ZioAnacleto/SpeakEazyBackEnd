package com.zioanacleto.home

import com.zioanacleto.cocktails.CocktailService
import com.zioanacleto.dbQuery
import org.jetbrains.exposed.sql.Database

class HomeService(private val database: Database) {

    suspend fun homeSections(): ExposedHomeSectionsList = dbQuery {
        val config = HomeConfigManager.loadConfig()
        val allSections =
            config.sections.filterOutBanner().shuffled().take(config.numberOfSections)
        val bannerSection = config.sections.findBanner()
        val cocktailService = CocktailService(database)

        var id = 0
        val exposedSections = allSections.map { section ->
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