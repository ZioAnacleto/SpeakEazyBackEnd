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
) {
    println("Api call in GET: ${call.request.uri}")
    val response = specificApiBlock()
    println("Response: $response")
}

suspend fun <Request: Any, Response> RoutingContext.basePostApi(
    requestClazz: KClass<Request>,
    specificApiBlock: suspend RoutingContext.() -> Response
) = baseApiWithRequestAndResponse("POST", requestClazz, specificApiBlock)

suspend fun <Request: Any, Response> RoutingContext.baseDeleteApi(
    requestClazz: KClass<Request>,
    specificApiBlock: suspend RoutingContext.() -> Response
) = baseApiWithRequestAndResponse("DELETE", requestClazz, specificApiBlock)

suspend fun <Response> RoutingContext.basePutApi(
    specificApiBlock: suspend RoutingContext.() -> Response
) {
    println("Api call in PUT: ${call.request.uri}")
    val response = specificApiBlock()
    println("Response: $response")
}

private suspend fun <Request: Any, Response> RoutingContext.baseApiWithRequestAndResponse(
    verb: String,
    requestClazz: KClass<Request>,
    specificApiBlock: suspend RoutingContext.() -> Response
) {
    println("Api call in $verb: ${call.request.uri}")
    println("Request: ${call.receive(requestClazz)}")
    val response = specificApiBlock()
    println("Response: $response")
}