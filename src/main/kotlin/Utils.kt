package com.zioanacleto

import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }

fun String?.default(value: String? = null): String = this ?: value ?: ""

suspend fun <Response> RoutingContext.baseGetApi(
    specificApiBlock: suspend RoutingContext.() -> Response
) {
    println("Api call in GET: ${call.request.uri}")
    val response = specificApiBlock()
    println("Response: $response")
}

suspend fun <Response> RoutingContext.basePostApi(
    specificApiBlock: suspend RoutingContext.() -> Response
) = baseApiWithRequestAndResponse("POST", specificApiBlock)

suspend fun <Response> RoutingContext.baseDeleteApi(
    specificApiBlock: suspend RoutingContext.() -> Response
) = baseApiWithRequestAndResponse("DELETE", specificApiBlock)

suspend fun <Response> RoutingContext.basePutApi(
    specificApiBlock: suspend RoutingContext.() -> Response
) = baseApiWithRequestAndResponse("PUT", specificApiBlock)

private suspend fun <Response> RoutingContext.baseApiWithRequestAndResponse(
    verb: String,
    specificApiBlock: suspend RoutingContext.() -> Response
) {
    println("Api call in $verb: ${call.request.uri}")
    println("Request: ${call.receive<Any>()}")
    val response = specificApiBlock()
    println("Response: $response")
}