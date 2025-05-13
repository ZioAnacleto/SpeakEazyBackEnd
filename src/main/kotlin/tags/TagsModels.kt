package com.zioanacleto.tags

import kotlinx.serialization.Serializable

@Serializable
data class ExposedTagList(
    val tags: List<ExposedTag>
)

@Serializable
data class ExposedTag(
    val id: String,
    val name: String
)