package com.example

import org.jetbrains.exposed.sql.Database

fun configureDatabases(): Database = Database.connect(
    url = "jdbc:postgresql://postgres.railway.internal:5432/railway",
    user = System.getenv("PGUSER"),
    driver = "org.postgresql.Driver",
    password = System.getenv("PGPASSWORD")
)
