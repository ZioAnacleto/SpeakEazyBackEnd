package com.zioanacleto

import org.jetbrains.exposed.sql.Database

fun configureDatabase(): Database = Database.connect(
    url = System.getenv("PGDATABASE_URL"),
    user = System.getenv("PGUSER"),
    driver = "org.postgresql.Driver",
    password = System.getenv("PGPASSWORD")
)
