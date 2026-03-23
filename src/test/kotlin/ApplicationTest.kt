package com.zioanacleto

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun testRoot() = testApplication {
        // generic setup
        application { module() }
        client.get("/").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }
}
