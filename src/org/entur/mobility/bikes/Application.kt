package org.entur.mobility.bikes

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.request.host
import io.ktor.request.port
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.jetty.Jetty
import java.time.LocalDateTime
import kotlin.concurrent.thread
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.entur.mobility.bikes.GbfsStandardEnum.Companion.getFetchUrl
import org.entur.mobility.bikes.bikeOperators.KolumbusResponse
import org.entur.mobility.bikes.bikeOperators.KolumbusStation
import org.entur.mobility.bikes.bikeOperators.Operator
import org.entur.mobility.bikes.bikeOperators.Operator.Companion.isUrbanSharing
import org.entur.mobility.bikes.bikeOperators.getOperatorsWithDiscovery
import org.entur.mobility.bikes.bikeOperators.toStationInformation
import org.entur.mobility.bikes.bikeOperators.toStationStatus
import org.entur.mobility.bikes.bikeOperators.toSystemInformation
import org.slf4j.LoggerFactory

fun main() {
    val server = embeddedServer(Jetty, watchPaths = listOf("bikeservice"), port = 8080, module = Application::module)
    server.start(wait = true)
}

fun Application.module() {
    val cache = InMemoryCache(HashMap(), LocalDateTime.now())

    thread(start = true) {
        launch { poll(cache) }
    }

    routing {
        get("/") {
            val host = call.request.host()
            val port = call.request.port()
            call.respondText(Gson().toJson(getOperatorsWithDiscovery(host, port)), ContentType.Application.Json)
        }

        get("/health") {
            call.respondText("OK")
        }

        get("{operator}/gbfs.json") {
            val operator = Operator.valueOf(call.parameters["operator"]?.toUpperCase() ?: throw NullPointerException())
            val gbfsEndpoints = getGbfsEndpoint(operator, call.request.host(), call.request.port())
            val response = getDiscovery(gbfsEndpoints)
            call.respondText(Gson().toJson(response), ContentType.Application.Json)
        }

        get("{operator}/{service}.json") {
            val operator = Operator.valueOf(call.parameters["operator"]?.toUpperCase() ?: throw NullPointerException())
            val gbfsEnum = GbfsStandardEnum.valueOf(call.parameters["service"] ?: throw NullPointerException())
            if (!cache.isValidCache(operator, gbfsEnum)) {
                fetchAndStoreInCache(
                    cache = cache,
                    operator = operator,
                    gbfsStandardEnum = gbfsEnum,
                    isPolling = false
                )
            }
            val result = cache.getResponseFromCache(operator, gbfsEnum)
            call.respondText(Gson().toJson(result), ContentType.Application.Json)
        }
    }
}

suspend inline fun <reified T> parseResponse(url: String): T {
    with(HttpClient()) {
        val response = get<String>(url) { header("Client-Identifier", "entur-bikeservice") }
        return Gson().fromJson(response, T::class.java)
    }
}

suspend inline fun parseKolumbusResponse(url: String): List<KolumbusStation> {
    with(HttpClient()) {
        val response = get<String>(url) { header("Client-Identifier", "entur-bikeservice") }
        val itemType = object : TypeToken<List<KolumbusStation>>() {}.type
        return Gson().fromJson(response, itemType)
    }
}

suspend inline fun poll(cache: InMemoryCache) {
    val logger = LoggerFactory.getLogger("org.entur.mobility.bikes")
    while (true) {
        Operator.values().forEach { operator ->
            logger.info("Polling $operator")
            GbfsStandardEnum.values().forEach { gbfsEnum ->
                fetchAndStoreInCache(
                    cache = cache,
                    operator = operator,
                    gbfsStandardEnum = gbfsEnum,
                    isPolling = true
                )
            }
        }
        delay(POLL_INTERVAL)
    }
}

suspend fun fetchAndStoreInCache(
    cache: InMemoryCache,
    operator: Operator,
    gbfsStandardEnum: GbfsStandardEnum,
    isPolling: Boolean
) {
    if (operator.isUrbanSharing()) {
        when (gbfsStandardEnum) {
            GbfsStandardEnum.gbfs -> {
            }
            GbfsStandardEnum.system_information -> {
                val response =
                    parseResponse<GBFSResponse.SystemInformationResponse>(gbfsStandardEnum.getFetchUrl(operator))
                cache.setResponseInCacheAndGet(operator, gbfsStandardEnum, response)
            }
            GbfsStandardEnum.station_information -> {
                val response = parseResponse<GBFSResponse.StationsResponse>(gbfsStandardEnum.getFetchUrl(operator)).toNeTEx(operator)
                cache.setResponseInCacheAndGet(operator, gbfsStandardEnum, response)
            }
            GbfsStandardEnum.station_status -> {
                val response =
                    parseResponse<GBFSResponse.StationStatusesResponse>(gbfsStandardEnum.getFetchUrl(operator)).toNeTEx(operator)
                cache.setResponseInCacheAndGet(operator, gbfsStandardEnum, response)
            }
        }
    } else {
        when (gbfsStandardEnum) {
            GbfsStandardEnum.gbfs -> {
            }
            GbfsStandardEnum.station_information -> {
                val response = KolumbusResponse(
                    data = parseKolumbusResponse(
                        gbfsStandardEnum.getFetchUrl(operator)
                    )
                )
                cache.setResponseInCacheAndGet(
                    operator,
                    GbfsStandardEnum.system_information,
                    response.toSystemInformation()
                )
                cache.setResponseInCacheAndGet(
                    operator,
                    GbfsStandardEnum.station_status,
                    response.toStationStatus().toNeTEx(operator)
                )
                cache.setResponseInCacheAndGet(
                    operator,
                    GbfsStandardEnum.station_information,
                    response.toStationInformation().toNeTEx(operator)
                )
            }
            GbfsStandardEnum.system_information -> if (!isPolling) {
                val response = KolumbusResponse(
                    data = parseKolumbusResponse(
                        gbfsStandardEnum.getFetchUrl(operator)
                    )
                )
                cache.setResponseInCacheAndGet(
                    operator,
                    GbfsStandardEnum.system_information,
                    response.toSystemInformation()
                )
            }
            GbfsStandardEnum.station_status -> if (!isPolling) {
                val response = KolumbusResponse(
                    data = parseKolumbusResponse(
                        gbfsStandardEnum.getFetchUrl(operator)
                    )
                )
                cache.setResponseInCacheAndGet(
                    operator,
                    GbfsStandardEnum.station_status,
                    response.toStationStatus().toNeTEx(operator)
                )
            }
        }
    }
}
