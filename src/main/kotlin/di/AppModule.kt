package com.zioanacleto.di

import com.zioanacleto.cocktails.InstructionsTranslator
import com.zioanacleto.cocktails.repository.CocktailRepositoryImpl
import com.zioanacleto.cocktails.repository.CocktailsRepository
import com.zioanacleto.cocktails.service.CocktailsService
import com.zioanacleto.configureClient
import com.zioanacleto.configureDatabase
import com.zioanacleto.home.HomeService
import com.zioanacleto.ingredients.repository.IngredientsRepository
import com.zioanacleto.ingredients.repository.IngredientsRepositoryImpl
import com.zioanacleto.ingredients.service.IngredientsService
import com.zioanacleto.search.SearchService
import com.zioanacleto.tags.repository.TagsRepository
import com.zioanacleto.tags.repository.TagsRepositoryImpl
import com.zioanacleto.tags.service.TagsService
import org.jetbrains.exposed.sql.Database
import org.koin.dsl.module

val appModule = module {
    // Singletons
    single { configureClient() }
    single { InstructionsTranslator(get()) }
    single<Database> { configureDatabase() }

    // Services
    single { CocktailsService(get(), get()) }
    single { IngredientsService(get()) }
    single { TagsService(get()) }
    single { SearchService(get(), get(), get(), get()) }
    single { HomeService(get()) }

    // Repositories
    single<CocktailsRepository> { CocktailRepositoryImpl(get(), get()) }
    single<IngredientsRepository> { IngredientsRepositoryImpl(get()) }
    single<TagsRepository> { TagsRepositoryImpl(get()) }
}