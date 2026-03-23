package com.zioanacleto

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.Calendar
import kotlin.math.log

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
    val log = call.application.log
    val path = call.request.uri
    val ip = call.request.origin.remoteHost
    val apiKeyName = call.request.headers["X-API-Key"]

    log.info(
        "Request - method={} path={} ip={} apiKey={}",
        verb,
        path,
        ip,
        apiKeyName
    )

    val start = System.currentTimeMillis()
    val response = specificApiBlock()
    val duration = System.currentTimeMillis() - start

    log.info(
        "API response - method={} path={} duration={}ms response={}",
        verb,
        path,
        duration,
        response
    )
}

private suspend fun <Request, Response> RoutingContext.baseApiWithRequestAndResponse(
    verb: String,
    request: Request,
    specificApiBlock: suspend RoutingContext.() -> Response
) {
    val log = call.application.log
    val path = call.request.uri
    val ip = call.request.origin.remoteHost
    val apiKeyName = call.request.headers["X-API-Key"]

    log.info(
        "Request - method={} path={} ip={} apiKey={}, request={}",
        verb,
        path,
        ip,
        apiKeyName,
        request
    )

    val start = System.currentTimeMillis()
    val response = specificApiBlock()
    val duration = System.currentTimeMillis() - start

    log.info(
        "API response - method={} path={} duration={}ms response={}",
        verb,
        path,
        duration,
        response
    )
}

suspend fun <Response> asyncCall(block: suspend CoroutineScope.() -> Response) =
    coroutineScope { async { block() }.await() }

fun RoutingCall.setCacheControl(fallback: Int = CACHE_CONTROL_ONE_HOUR) = run {
    val cacheControlHeader = request.headers[HttpHeaders.CacheControl]

    cacheControlHeader?.let { header ->
        // look for max-age=XXXX
        val maxAge = Regex("max-age=(\\d+)")
            .find(header)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()

        response.cacheControl(
            CacheControl.MaxAge(maxAge ?: fallback)
        )
    }
}

const val CACHE_CONTROL_ONE_HOUR = (1 * 60 * 60)
const val CACHE_CONTROL_ONE_MINUTE = (1 * 60)