package com.zioanacleto.home

import com.zioanacleto.cocktails.ExposedCocktail
import kotlinx.serialization.Serializable

@Serializable
data class ExposedHomeSectionsList(
    val sections: List<ExposedHomeSection>,
    val banner: ExposedBanner? = null
)

@Serializable
data class ExposedHomeSection(
    val id: String,
    val name: String,
    val cocktails: List<ExposedCocktail>
)

@Serializable
data class ExposedBanner(
    val position: String,
    val name: String,
    val cocktailInfo: ExposedCocktail,
    val cta: String? = null
)