package com.zioanacleto.tags.service

import com.zioanacleto.tags.ExposedTagList

interface TagsService {
    suspend fun readAll(): ExposedTagList
}