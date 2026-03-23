package com.zioanacleto.tags.repository

import com.zioanacleto.tags.ExposedTagList

interface TagsRepository {
    suspend fun readAll(): ExposedTagList
}