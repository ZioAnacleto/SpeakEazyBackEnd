package com.zioanacleto

import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.Calendar
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
    println("Api call in $verb: ${call.request.uri}, time: ${Calendar.getInstance().time}")
    val response = specificApiBlock()
    println("Response: $response, time: ${Calendar.getInstance().time}")
}

private suspend fun <Request, Response> RoutingContext.baseApiWithRequestAndResponse(
    verb: String,
    request: Request,
    specificApiBlock: suspend RoutingContext.() -> Response
) {
    println("Api call in $verb: ${call.request.uri}, time: ${Calendar.getInstance().time}")
    println("Request: $request")
    val response = specificApiBlock()
    println("Response: $response, time: ${Calendar.getInstance().time}")
}

suspend fun <Response> asyncCall(block: suspend CoroutineScope.() -> Response) =
    coroutineScope { async { block() }.await() }