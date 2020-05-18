package org.entur.mobility.bikes

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.metrics.micrometer.MicrometerMetrics
import io.ktor.request.host
import io.ktor.request.port
import io.ktor.response.header
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.jetty.Jetty
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import java.lang.Exception
import kotlin.concurrent.thread
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.entur.mobility.bikes.GbfsStandardEnum.Companion.getFetchUrl
import org.entur.mobility.bikes.bikeOperators.KolumbusResponse
import org.entur.mobility.bikes.bikeOperators.KolumbusStation
import org.entur.mobility.bikes.bikeOperators.Operator
import org.entur.mobility.bikes.bikeOperators.Operator.Companion.isUrbanSharing
import org.entur.mobility.bikes.bikeOperators.getOperatorsWithDiscovery
import org.entur.mobility.bikes.bikeOperators.kolumbusBysykkelURL
import org.entur.mobility.bikes.bikeOperators.toStationInformation
import org.entur.mobility.bikes.bikeOperators.toStationStatus
import org.entur.mobility.bikes.bikeOperators.toSystemInformation
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val logger: Logger = LoggerFactory.getLogger("org.entur.mobility.bikes")

fun main() {
    val server = embeddedServer(Jetty, watchPaths = listOf("bikeservice"), port = 8080, module = Application::module)
    server.start(wait = true)
}

fun Application.module() {
    val cache = InMemoryCache(HashMap())
    val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    thread(start = true) {
        launch { poll(cache) }
    }
    install(MicrometerMetrics) {
        registry = meterRegistry
    }
    routing {
        get("/") {
            val host = call.request.host()
            val port = call.request.port()
            val correlationId = call.request.headers.get("x-correlation-id")

            if (correlationId != null) call.response.header("x-correlation-id", correlationId)
            call.respondText(Gson().toJson(getOperatorsWithDiscovery(host, port)), ContentType.Application.Json)
        }

        get("/health") {
            call.respondText("OK")
        }

        get("/actuator/prometheus") {
            call.respondText(meterRegistry.scrape())
        }

        get("{operator}/gbfs.json") {
            val correlationId = call.request.headers.get("x-correlation-id")
            val operator = Operator.valueOf(call.parameters["operator"]?.toUpperCase() ?: throw NullPointerException())
            val gbfsEndpoints = getGbfsEndpoint(operator, call.request.host(), call.request.port())
            val response = getDiscovery(gbfsEndpoints)

            if (correlationId != null) call.response.header("x-correlation-id", correlationId)
            call.respondText(Gson().toJson(response), ContentType.Application.Json)
        }

        get("{operator}/{service}.json") {
            val operator = Operator.valueOf(call.parameters["operator"]?.toUpperCase() ?: throw NullPointerException())
            val correlationId = call.request.headers.get("x-correlation-id")
            val gbfsEnum = GbfsStandardEnum.valueOf(call.parameters["service"] ?: throw NullPointerException())
            if (!cache.isValidCache(operator, gbfsEnum)) {
                try {
                    fetchAndStoreInCache(
                        cache = cache,
                        operator = operator,
                        gbfsStandardEnum = gbfsEnum
                    )
                } catch (e: Exception) {
                    logger.error("Failed to fetch $gbfsEnum from $operator. $e")
                }
            }
            val result = cache.getResponseFromCache(operator, gbfsEnum)
            if (correlationId != null) call.response.header("x-correlation-id", correlationId)
            call.respondText(Gson().toJson(result), ContentType.Application.Json)
        }
    }
}

suspend inline fun <reified T> parseResponse(url: String): T {
    val client = HttpClient()
    val response = client.get<String>(url) { header("Client-Identifier", "entur-bikeservice") }
    client.close()
    return Gson().fromJson(response, T::class.java)
}

suspend inline fun parseKolumbusResponse(): List<KolumbusStation> {
    val client = HttpClient()
    val response = client.get<String>(kolumbusBysykkelURL.getValue(GbfsStandardEnum.system_information)) {
            header(
                "Client-Identifier",
                "entur-bikeservice"
            )
        }
    client.close()
    val itemType = object : TypeToken<List<KolumbusStation>>() {}.type
    return Gson().fromJson(response, itemType)
}

suspend inline fun poll(cache: InMemoryCache) {
    while (true) {
        Operator.values().forEach { operator ->
            logger.info("Polling $operator")
            try {
                if (operator.isUrbanSharing()) {
                    GbfsStandardEnum.values().forEach { gbfsEnum ->
                        fetchAndStoreInCache(
                            cache = cache,
                            operator = operator,
                            gbfsStandardEnum = gbfsEnum
                        )
                    }
                } else {
                    fetchAndStoreInCache(cache, operator, GbfsStandardEnum.gbfs)
                }
            } catch (e: Exception) {
                logger.error("Failed to poll from $operator. $e")
            }
        }
        delay(POLL_INTERVAL_MS)
    }
}

suspend fun fetchAndStoreInCache(
    cache: InMemoryCache,
    operator: Operator,
    gbfsStandardEnum: GbfsStandardEnum
) {
    if (operator.isUrbanSharing()) {
        val response = when (gbfsStandardEnum) {
            GbfsStandardEnum.gbfs -> {
                null
            }
            GbfsStandardEnum.system_information -> {
                parseResponse<GBFSResponse.SystemInformationResponse>(gbfsStandardEnum.getFetchUrl(operator))
            }
            GbfsStandardEnum.station_information -> {
                parseResponse<GBFSResponse.StationsResponse>(gbfsStandardEnum.getFetchUrl(operator)).toNeTEx(
                    operator
                )
            }
            GbfsStandardEnum.station_status -> {
                parseResponse<GBFSResponse.StationStatusesResponse>(gbfsStandardEnum.getFetchUrl(operator)).toNeTEx(
                    operator
                )
            }
        }
        if (response != null) cache.setResponseInCacheAndGet(operator, gbfsStandardEnum, response)
    } else { // else it is Kolumbus
        val response = KolumbusResponse(parseKolumbusResponse())
        cache.setResponseInCacheAndGet(operator, GbfsStandardEnum.system_information, response.toSystemInformation())
        cache.setResponseInCacheAndGet(
            operator,
            GbfsStandardEnum.station_information,
            response.toStationInformation().toNeTEx(operator)
        )
        cache.setResponseInCacheAndGet(
            operator,
            GbfsStandardEnum.station_status,
            response.toStationStatus().toNeTEx(operator)
        )
    }
}
