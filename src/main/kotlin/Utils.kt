package com.zioanacleto

import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlin.reflect.KClass

suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }

fun String?.default(value: String? = null): String = this ?: value ?: ""

suspend fun <Response> RoutingContext.baseGetApi(
    specificApiBlock: suspend RoutingContext.() -> Response
) = baseApiWithResponse("GET", specificApiBlock)

suspend fun <Request, Response> RoutingContext.basePostApi(
    request: Request,
    specificApiBlock: suspend RoutingContext.() -> Response
) = baseApiWithRequestAndResponse("POST", request, specificApiBlock)

suspend fun <Request, Response> RoutingContext.baseDeleteApi(
    request: Request,
    specificApiBlock: suspend RoutingContext.() -> Response
) = baseApiWithRequestAndResponse("DELETE", request, specificApiBlock)

suspend fun <Response> RoutingContext.basePutApi(
    specificApiBlock: suspend RoutingContext.() -> Response
) = baseApiWithResponse("PUT", specificApiBlock)

private suspend fun <Response> RoutingContext.baseApiWithResponse(
    verb: String,
    specificApiBlock: suspend RoutingContext.() -> Response
) {
    println("Api call in $verb: ${call.request.uri}")
    val response = specificApiBlock()
    println("Response: $response")
}

private suspend fun <Request, Response> RoutingContext.baseApiWithRequestAndResponse(
    verb: String,
    request: Request,
    specificApiBlock: suspend RoutingContext.() -> Response
) {
    println("Api call in $verb: ${call.request.uri}")
    println("Request: $request")
    val response = specificApiBlock()
    println("Response: $response")
}