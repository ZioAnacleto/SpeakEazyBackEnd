package com.zioanacleto.tags.service

import com.zioanacleto.tags.ExposedTagList
import com.zioanacleto.tags.repository.TagsRepository

class TagsServiceImpl(
    private val repository: TagsRepository
) : TagsService {
    override suspend fun readAll(): ExposedTagList = repository.readAll()
}