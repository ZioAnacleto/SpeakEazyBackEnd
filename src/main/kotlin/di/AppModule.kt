package com.zioanacleto.di

import com.zioanacleto.admin.EnvironmentKeysProvider
import com.zioanacleto.admin.EnvironmentKeysProviderImpl
import com.zioanacleto.cocktails.InstructionsTranslator
import com.zioanacleto.cocktails.repository.CocktailRepositoryImpl
import com.zioanacleto.cocktails.repository.CocktailsRepository
import com.zioanacleto.cocktails.service.CocktailsService
import com.zioanacleto.cocktails.service.CocktailsServiceImpl
import com.zioanacleto.configureClient
import com.zioanacleto.configureDatabase
import com.zioanacleto.home.HomeService
import com.zioanacleto.home.provider.HomeConfigProvider
import com.zioanacleto.home.provider.HomeConfigProviderImpl
import com.zioanacleto.ingredients.repository.IngredientsRepository
import com.zioanacleto.ingredients.repository.IngredientsRepositoryImpl
import com.zioanacleto.ingredients.service.IngredientsService
import com.zioanacleto.ingredients.service.IngredientsServiceImpl
import com.zioanacleto.search.service.SearchService
import com.zioanacleto.search.service.SearchServiceImpl
import com.zioanacleto.tags.repository.TagsRepository
import com.zioanacleto.tags.repository.TagsRepositoryImpl
import com.zioanacleto.tags.service.TagsService
import com.zioanacleto.tags.service.TagsServiceImpl
import org.jetbrains.exposed.sql.Database
import org.koin.dsl.module

val appModule = module {
    // Singletons
    single { configureClient() }
    single { InstructionsTranslator(get(), get()) }
    single<Database> { configureDatabase() }
    single<HomeConfigProvider> { HomeConfigProviderImpl() }
    single<EnvironmentKeysProvider> { EnvironmentKeysProviderImpl() }

    // Services
    single<CocktailsService> { CocktailsServiceImpl(get(), get()) }
    single<IngredientsService> { IngredientsServiceImpl(get()) }
    single<TagsService> { TagsServiceImpl(get()) }
    single<SearchService> { SearchServiceImpl(get(), get(), get(), get(), get()) }
    single { HomeService(get(), get()) }

    // Repositories
    single<CocktailsRepository> { CocktailRepositoryImpl(get(), get()) }
    single<IngredientsRepository> { IngredientsRepositoryImpl(get()) }
    single<TagsRepository> { TagsRepositoryImpl(get()) }
}