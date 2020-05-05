package org.entur

import BikeResponse
import BikeResponseData
import BikeResponseFeed
import BikeResponseLanguage
import StationInformationResponse
import StationStatusResponse
import SystemInformationResponse
import com.google.gson.Gson
import getOperatorName
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
import java.lang.NullPointerException
import java.time.LocalDateTime
import org.entur.bikeOperators.UrbanSharingOperator
import org.entur.bikeOperators.getUrbanSharingOperator
import org.entur.bikeOperators.getUrbanSharingOperators

fun main() {
    val server = embeddedServer(Jetty, watchPaths = listOf("bikeservice"), port = 8080, module = Application::module)
    server.start(wait = true)
}

fun Application.module() {
    val gbfsCache = InMemoryCache<BikeResponse>(HashMap(), LocalDateTime.now())
    val systemInformationCache = InMemoryCache<SystemInformationResponse>(HashMap(), LocalDateTime.now())
    val stationInformationCache = InMemoryCache<StationInformationResponse>(HashMap(), LocalDateTime.now())
    val stationStatusCache = InMemoryCache<StationStatusResponse>(HashMap(), LocalDateTime.now())

    routing {
        get("/") {
            call.respondText("Hello and welcome to Entur Bikeservice!", ContentType.Application.Json)
        }
        get("{operator}/gbfs.json") {
            val operator = UrbanSharingOperator.valueOf(call.parameters["operator"]?.toUpperCase() ?: throw NullPointerException())
            val result = if (gbfsCache.isValidCache(operator)) {
                gbfsCache.getResponseFromCache(operator)
            } else {
                val host = call.request.host()
                val port = call.request.port()
                val response = formatGbfsEndpoints(parseResponse<BikeResponse>(
                    getUrbanSharingOperator(
                        operator
                    ).gbfs), host, port)
                gbfsCache.setResponseInCache(operator, response)
                response
            }
            call.respondText(Gson().toJson(result), ContentType.Application.Json)
        }
        get("{operator}/system_information.json") {
            val operator = UrbanSharingOperator.valueOf(call.parameters["operator"]?.toUpperCase() ?: throw NullPointerException())
            val result = if (systemInformationCache.isValidCache(operator)) {
                systemInformationCache.getResponseFromCache(operator)
            } else {
                val response = parseResponse<SystemInformationResponse>(
                    getUrbanSharingOperator(operator).system_information)
                systemInformationCache.setResponseInCache(operator, response)
                response
            }
            call.respondText(Gson().toJson(result), ContentType.Application.Json)
        }
        get("{operator}/station_information.json") {
            val operator = UrbanSharingOperator.valueOf(call.parameters["operator"]?.toUpperCase() ?: throw NullPointerException())
            val result = if (stationInformationCache.isValidCache(operator)) {
                stationInformationCache.getResponseFromCache(operator)
            } else {
                val response = parseResponse<StationInformationResponse>(
                    getUrbanSharingOperator(operator).station_information)
                stationInformationCache.setResponseInCache(operator, response)
                response
            }
            call.respondText(Gson().toJson(result), ContentType.Application.Json)
        }
        get("{operator}/station_status.json") {
            val operator = UrbanSharingOperator.valueOf(call.parameters["operator"]?.toUpperCase() ?: throw NullPointerException())
            val result = if (stationStatusCache.isValidCache(operator)) {
                stationStatusCache.getResponseFromCache(operator)
            } else {
                val response = parseResponse<StationStatusResponse>(
                    getUrbanSharingOperator(operator).station_status)
                stationStatusCache.setResponseInCache(operator, response)
                response
            }
            call.respondText(Gson().toJson(result), ContentType.Application.Json)
        }
        get("/all") {
            val host = call.request.host()
            val port = call.request.port()
            call.respondText(Gson().toJson(getUrbanSharingOperators(host, port)), ContentType.Application.Json)
        }
    }
}

suspend inline fun <reified T> parseResponse(url: String): T {
    with(HttpClient()) {
        val response = get<String>(url) { header("Client-Identifier", "entur-bikeservice") }
        return Gson().fromJson(response, T::class.java)
    }
}

inline fun formatGbfsEndpoints(bikeResponse: BikeResponse, host: String, port: Int): BikeResponse =
    BikeResponse(
        last_updated = bikeResponse.last_updated,
        ttl = bikeResponse.ttl,
        data = BikeResponseData(
            nb = BikeResponseLanguage(
                feeds = bikeResponse.data.nb.feeds.map {
                    BikeResponseFeed(
                        name = it.name,
                        url = formatUrl(it.url, host, port)
                    )
                }
            )
        )
    )

inline fun formatUrl(url: String, host: String, port: Int): String {
    var splittedUrl = url.split('/').toMutableList()
    val operator = getOperatorName(splittedUrl[3])
    splittedUrl[2] = if (host.equals("localhost")) "$host:$port" else host
    splittedUrl[3] = operator
    splittedUrl = splittedUrl.slice(2 until splittedUrl.size).toMutableList()

    return splittedUrl.joinToString(separator = "/")
}
