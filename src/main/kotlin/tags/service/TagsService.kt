package com.zioanacleto.tags.service

import com.zioanacleto.tags.ExposedTagList
import com.zioanacleto.tags.repository.TagsRepository

class TagsService(private val repository: TagsRepository) {
    suspend fun readAll(): ExposedTagList = repository.readAll()
}