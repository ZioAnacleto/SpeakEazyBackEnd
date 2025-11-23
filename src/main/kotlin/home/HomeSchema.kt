package com.zioanacleto.home

import com.zioanacleto.cocktails.CocktailService
import com.zioanacleto.dbQuery
import org.jetbrains.exposed.sql.Database

class HomeService(private val database: Database) {

    suspend fun homeSections(): ExposedHomeSectionsList = dbQuery {
        val config = HomeConfigManager.loadConfig()
        val allSections = config.sections.shuffled().take(config.numberOfSections)
        val bannerSection = config.sections.firstOrNull { it.type == "banner" }
        val cocktailService = CocktailService(database)

        var id = 0
        val exposedSections = allSections.map { section ->
            val cocktails = when(section.type) {
                "category" -> cocktailService.readAllWithCategory(section.query!!).cocktails
                "type" -> cocktailService.readAllWithType(section.query!!).cocktails
                "popular" -> cocktailService.readMostPopular(section.limit!!).cocktails
                "ingredient" -> cocktailService.readAllWithIngredient(section.query!!).cocktails
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
            ExposedBanner(
                position = section.position!!,
                name = section.title,
                cocktailInfo = cocktail!!,
                cta = section.cta
            )
        }

        ExposedHomeSectionsList(exposedSections, banner)
    }
}