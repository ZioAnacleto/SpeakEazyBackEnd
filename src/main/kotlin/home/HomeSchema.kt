package com.zioanacleto.home

import com.zioanacleto.cocktails.CocktailService
import com.zioanacleto.dbQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.jetbrains.exposed.sql.Database

class HomeService(private val database: Database) {

    suspend fun homeSections(): ExposedHomeSectionsList = dbQuery {
        var id = 0
        coroutineScope {
            val deferredUnforgettables = async(Dispatchers.IO) {
                CocktailService(database).readAllWithCategory(QUERY_COCKTAIL_CATEGORY).cocktails
            }.await()
            val deferredBeforeDinners = async(Dispatchers.IO) {
                CocktailService(database).readAllWithType(QUERY_COCKTAIL_TYPE).cocktails
            }.await()
            val deferredMostPopular = async(Dispatchers.IO) {
                CocktailService(database).readMostPopular(QUERY_COCKTAIL_POPULAR_SIZE).cocktails
            }.await()

            ExposedHomeSectionsList(
                listOf(
                    ExposedHomeSection(
                        id = (++id).toString(),
                        name = "IBA: The Unforgettables",
                        cocktails = deferredUnforgettables
                    ),
                    ExposedHomeSection(
                        id = (++id).toString(),
                        name = "Before Dinner Cocktails",
                        cocktails = deferredBeforeDinners
                    ),
                    ExposedHomeSection(
                        id = (++id).toString(),
                        name = "I più popolari",
                        cocktails = deferredMostPopular
                    )
                )
            )
        }
    }

    companion object {
        private const val QUERY_COCKTAIL_CATEGORY = "The Unforgettables"
        private const val QUERY_COCKTAIL_TYPE = "Before Dinner"
        private const val QUERY_COCKTAIL_POPULAR_SIZE = 10
    }
}