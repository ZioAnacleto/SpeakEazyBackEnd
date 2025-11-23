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

@Serializable
data class HomeSectionsConfig(
    val sections: List<SectionConfig>,
    val numberOfSections: Int
)

@Serializable
data class SectionConfig(
    val type: String,
    val query: String? = null,
    val limit: Int? = null,
    val title: String,
    val cta: String? = null,
    val position: String? = null
)