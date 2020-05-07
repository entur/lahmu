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
import org.entur.mobility.bikes.bikeOperators.KolumbusResponse
import org.entur.mobility.bikes.bikeOperators.KolumbusStation
import org.entur.mobility.bikes.bikeOperators.Operator
import org.entur.mobility.bikes.bikeOperators.Operator.Companion.getFetchUrls
import org.entur.mobility.bikes.bikeOperators.getOperatorsWithDiscovery
import org.entur.mobility.bikes.bikeOperators.toStationInformation
import org.entur.mobility.bikes.bikeOperators.toStationStatus
import org.entur.mobility.bikes.bikeOperators.toSystemInformation

fun main() {
    val server = embeddedServer(Jetty, watchPaths = listOf("bikeservice"), port = 8080, module = Application::module)
    server.start(wait = true)
}

fun Application.module() {
    val systemInformationCache =
        InMemoryCache<GBFSResponse.SystemInformationResponse>(
            HashMap(),
            LocalDateTime.now()
        )
    val stationInformationCache =
        InMemoryCache<GBFSResponse.StationsResponse>(
            HashMap(),
            LocalDateTime.now()
        )
    val stationStatusCache =
        InMemoryCache<GBFSResponse.StationStatusesResponse>(
            HashMap(),
            LocalDateTime.now()
        )

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
            val gbfsEndpoints = getGbfsEndpoint(
                operator,
                call.request.host(),
                call.request.port()
            )
            val response = getDiscovery(gbfsEndpoints)
            call.respondText(Gson().toJson(response), ContentType.Application.Json)
        }

        get("{operator}/system_information.json") {
            val operator = Operator.valueOf(call.parameters["operator"]?.toUpperCase() ?: throw NullPointerException())
            val result = when {
                systemInformationCache.isValidCache(operator) -> systemInformationCache.getResponseFromCache(operator)
                operator === Operator.KOLUMBUSBYSYKKEL -> {
                    val response = KolumbusResponse(
                        data = parseKolumbusResponse(
                            operator.getFetchUrls().system_information
                        )
                    ).toSystemInformation()
                    systemInformationCache.setResponseInCacheAndGet(operator, response)
                }
                else -> {
                    val response =
                        parseResponse<GBFSResponse.SystemInformationResponse>(
                            operator.getFetchUrls().system_information
                        )
                    systemInformationCache.setResponseInCacheAndGet(operator, response)
                }
            }
            call.respondText(Gson().toJson(result), ContentType.Application.Json)
        }

        get("{operator}/station_information.json") {
            val operator = Operator.valueOf(call.parameters["operator"]?.toUpperCase() ?: throw NullPointerException())
            val result = when {
                (stationInformationCache.isValidCache(operator)) -> stationInformationCache.getResponseFromCache(
                    operator
                )
                operator === Operator.KOLUMBUSBYSYKKEL -> {
                    val response = KolumbusResponse(
                        data = parseKolumbusResponse(
                            operator.getFetchUrls().station_information
                        )
                    ).toStationInformation().toNeTEx(operator)
                    stationInformationCache.setResponseInCacheAndGet(operator, response)
                }
                else -> {
                    val response = parseResponse<GBFSResponse.StationsResponse>(
                        operator.getFetchUrls().station_information
                    ).toNeTEx(operator)
                    stationInformationCache.setResponseInCacheAndGet(operator, response)
                }
            }
            call.respondText(Gson().toJson(result), ContentType.Application.Json)
        }

        get("{operator}/station_status.json") {
            val operator = Operator.valueOf(call.parameters["operator"]?.toUpperCase() ?: throw NullPointerException())
            val result = when {
                (stationStatusCache.isValidCache(operator)) -> stationStatusCache.getResponseFromCache(operator)
                operator === Operator.KOLUMBUSBYSYKKEL -> {
                    val response = KolumbusResponse(
                        data = parseKolumbusResponse(
                            operator.getFetchUrls().station_status
                        )
                    ).toStationStatus().toNeTEx(operator)
                    stationStatusCache.setResponseInCacheAndGet(operator, response)
                }
                else -> {
                    val response = parseResponse<GBFSResponse.StationStatusesResponse>(
                        operator.getFetchUrls().station_status
                    ).toNeTEx(operator)
                    stationStatusCache.setResponseInCacheAndGet(operator, response)
                }
            }
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
