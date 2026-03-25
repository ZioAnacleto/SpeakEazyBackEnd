package com.zioanacleto.home

import com.zioanacleto.cocktails.service.CocktailsService
import com.zioanacleto.home.provider.HomeConfigProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class HomeService(
    private val cocktailsService: CocktailsService,
    private val homeConfigProvider: HomeConfigProvider
) {

    suspend fun homeSections(): ExposedHomeSectionsList = coroutineScope {
        val config = homeConfigProvider.loadConfig()
        // take [numberOfSections] sections shuffled (including banners)
        val allSections = config.sections.shuffled().take(config.numberOfSections)
        // take only one banner (if any) from allSections
        val bannerSection = allSections.findBanner()

        var id = 0
        // mapping all sections but banner
        val sectionDeferreds = allSections.filterOutBanner().map { section ->
            async {
                val cocktails = when (section.type.toType()) {
                    SectionType.CATEGORY -> cocktailsService.readAllWithCategory(section.query!!).cocktails
                    SectionType.TYPE -> cocktailsService.readAllWithType(section.query!!).cocktails
                    SectionType.POPULAR -> cocktailsService.readMostPopular(section.limit!!).cocktails
                    SectionType.INGREDIENT -> cocktailsService.readAllWithIngredient(section.query!!).cocktails
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
            val cocktail = cocktailsService.readSingleWithName(section.query!!)
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