package com.zioanacleto.home

import com.zioanacleto.cocktails.CocktailService
import com.zioanacleto.dbQuery
import org.jetbrains.exposed.sql.Database

class HomeService(private val database: Database) {

    suspend fun homeSections(): ExposedHomeSectionsList = dbQuery {
        var id = 0
        val unforgettables = CocktailService(database).readAllWithCategory(QUERY_COCKTAIL_CATEGORY).cocktails
        val beforeDinners = CocktailService(database).readAllWithType(QUERY_COCKTAIL_TYPE).cocktails
        val mostPopular = CocktailService(database).readMostPopular(QUERY_COCKTAIL_POPULAR_SIZE).cocktails

        ExposedHomeSectionsList(
            listOf(
                ExposedHomeSection(
                    id = (++id).toString(),
                    name = "IBA: The Unforgettables",
                    cocktails = unforgettables
                ),
                ExposedHomeSection(
                    id = (++id).toString(),
                    name = "Before Dinner Cocktails",
                    cocktails = beforeDinners
                ),
                ExposedHomeSection(
                    id = (++id).toString(),
                    name = "I più popolari",
                    cocktails = mostPopular
                )
            )
        )
    }

    companion object {
        private const val QUERY_COCKTAIL_CATEGORY = "The Unforgettables"
        private const val QUERY_COCKTAIL_TYPE = "Before Dinner"
        private const val QUERY_COCKTAIL_POPULAR_SIZE = 10
    }
}