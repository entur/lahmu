package org.entur.mobility.bikes

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
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
import java.util.Timer
import java.util.UUID
import kotlin.concurrent.schedule
import kotlin.concurrent.thread
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.entur.mobility.bikes.GbfsStandardEnum.Companion.getFetchUrl
import org.entur.mobility.bikes.bikeOperators.DrammenAccessToken
import org.entur.mobility.bikes.bikeOperators.DrammenStationsResponse
import org.entur.mobility.bikes.bikeOperators.DrammenStationsStatusResponse
import org.entur.mobility.bikes.bikeOperators.JCDecauxResponse
import org.entur.mobility.bikes.bikeOperators.JCDecauxStation
import org.entur.mobility.bikes.bikeOperators.KolumbusResponse
import org.entur.mobility.bikes.bikeOperators.KolumbusStation
import org.entur.mobility.bikes.bikeOperators.Operator
import org.entur.mobility.bikes.bikeOperators.Operator.Companion.isDrammenSmartBike
import org.entur.mobility.bikes.bikeOperators.Operator.Companion.isJCDecaux
import org.entur.mobility.bikes.bikeOperators.Operator.Companion.isKolumbus
import org.entur.mobility.bikes.bikeOperators.Operator.Companion.isUrbanSharing
import org.entur.mobility.bikes.bikeOperators.drammenSystemInformation
import org.entur.mobility.bikes.bikeOperators.drammenSystemPricingPlan
import org.entur.mobility.bikes.bikeOperators.getOperatorsWithDiscovery
import org.entur.mobility.bikes.bikeOperators.jcDecauxSystemInformation
import org.entur.mobility.bikes.bikeOperators.jcDecauxSystemPricingPlans
import org.entur.mobility.bikes.bikeOperators.kolumbusBysykkelURL
import org.entur.mobility.bikes.bikeOperators.kolumbusSystemPricingPlans
import org.entur.mobility.bikes.bikeOperators.lillestromBysykkelURL
import org.entur.mobility.bikes.bikeOperators.toStationInformation
import org.entur.mobility.bikes.bikeOperators.toStationStatus
import org.entur.mobility.bikes.bikeOperators.toStationStatuses
import org.entur.mobility.bikes.bikeOperators.urbanSharingSystemPricePlan
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC

val logger: Logger = LoggerFactory.getLogger("org.entur.mobility.bikes")
val client = HttpClient()

fun main() {
    val server = embeddedServer(Jetty, watchPaths = listOf("bikeservice"), port = 8080, module = Application::module)
    server.start(wait = true)
}

fun Application.module() {
    val cache = InMemoryCache(HashMap())
    val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    thread(start = true) {
        Timer().schedule(0L, TIME_TO_LIVE_DRAMMEN_ACCESS_KEY_MS) {
            fetchAndSetDrammenAccessToken()
        }
        Timer().schedule(0L, POLL_INTERVAL_MS) {
            poll(cache)
        }
    }

    install(MicrometerMetrics) {
        registry = meterRegistry
    }
    routing {
        intercept(ApplicationCallPipeline.Call) {
            try {
                val httpRequestCorrelationId = call.request.headers.get("x-correlation-id")
                val correlationId =
                    if (httpRequestCorrelationId != null) sanitize(httpRequestCorrelationId) else UUID.randomUUID()
                        .toString()
                val requestId = UUID.randomUUID().toString()

                MDC.put("correlationId", correlationId)
                MDC.put("requestId", requestId)

                call.response.header("x-correlation-id", correlationId)
                call.response.header("x-request-id", requestId)

                runBlocking { proceed() }
            } finally {
                MDC.clear()
            }
        }
        get("/") {
            val host = call.request.host()
            val port = call.request.port()

            call.respondText(Gson().toJson(getOperatorsWithDiscovery(host, port)), ContentType.Application.Json)
        }

        get("/health") {
            call.respondText("OK")
        }

        get("/actuator/prometheus") {
            call.respondText(meterRegistry.scrape())
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
            call.respondText(Gson().toJson(result), ContentType.Application.Json)
        }
    }
}

inline fun <reified T> parseResponse(url: String): T {
    val response = fetch(url)
    return Gson().fromJson(response, T::class.java)
}

fun parseKolumbusResponse(): List<KolumbusStation> {
    val response = fetch(kolumbusBysykkelURL.getValue(GbfsStandardEnum.system_information))
    val itemType = object : TypeToken<List<KolumbusStation>>() {}.type
    return Gson().fromJson(response, itemType)
}

fun parseJCDecauxResponse(): List<JCDecauxStation> {
    val response = fetch(lillestromBysykkelURL.getValue(GbfsStandardEnum.system_information))
    val itemType = object : TypeToken<List<JCDecauxStation>>() {}.type
    return Gson().fromJson(response, itemType)
}

fun poll(cache: InMemoryCache) {
    Operator.values().forEach { operator ->
        GlobalScope.async {
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
                } else if (operator.isDrammenSmartBike()) {
                    fetchAndStoreInCache(cache, operator, GbfsStandardEnum.system_information)
                    fetchAndStoreInCache(cache, operator, GbfsStandardEnum.station_information)
                } else {
                    fetchAndStoreInCache(cache, operator, GbfsStandardEnum.gbfs)
                }
            } catch (e: Exception) {
                logger.error("Failed to poll from $operator. $e")
            }
        }
    }
}

fun fetchAndStoreInCache(
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
                parseResponse<GBFSResponse.StationsInformationResponse>(gbfsStandardEnum.getFetchUrl(operator)).toNeTEx(
                    operator
                )
            }
            GbfsStandardEnum.station_status -> {
                parseResponse<GBFSResponse.StationStatusesResponse>(gbfsStandardEnum.getFetchUrl(operator)).toNeTEx(
                    operator
                )
            }
            GbfsStandardEnum.system_pricing_plans -> urbanSharingSystemPricePlan(operator)
        }
        if (response != null) cache.setResponseInCacheAndGet(operator, gbfsStandardEnum, response)
    } else if (operator.isKolumbus()) {
        val response = KolumbusResponse(parseKolumbusResponse())
        cache.setResponseInCacheAndGet(
            operator,
            GbfsStandardEnum.system_information,
            response.jcDecauxSystemInformation()
        )
        cache.setResponseInCacheAndGet(
            operator,
            GbfsStandardEnum.station_information,
            response.toStationInformation()
        )
        cache.setResponseInCacheAndGet(
            operator,
            GbfsStandardEnum.station_status,
            response.toStationStatus()
        )
        cache.setResponseInCacheAndGet(
            operator,
            GbfsStandardEnum.system_pricing_plans,
            kolumbusSystemPricingPlans()
        )
    } else if (operator.isJCDecaux()) {
        cache.setResponseInCacheAndGet(
            operator,
            GbfsStandardEnum.system_pricing_plans,
            jcDecauxSystemPricingPlans()
        )
        cache.setResponseInCacheAndGet(
            operator,
            GbfsStandardEnum.system_information,
            jcDecauxSystemInformation()
        )
        val response = JCDecauxResponse(data = parseJCDecauxResponse())
        cache.setResponseInCacheAndGet(
            operator,
            GbfsStandardEnum.station_status,
            response.toStationStatus()
        )
        cache.setResponseInCacheAndGet(
            operator,
            GbfsStandardEnum.station_information,
            response.toStationInformation()
        )
    } else if (operator.isDrammenSmartBike()) {
        if (DRAMMEN_ACCESS_TOKEN == "") fetchAndSetDrammenAccessToken()
        val response = when (gbfsStandardEnum) {
            GbfsStandardEnum.gbfs -> {
                null
            }
            GbfsStandardEnum.system_information -> {
                drammenSystemInformation()
            }
            GbfsStandardEnum.station_information -> {
                val stationsStatusResponse = parseResponse<DrammenStationsStatusResponse>(
                    GbfsStandardEnum.station_status.getFetchUrl(
                        operator,
                        DRAMMEN_ACCESS_TOKEN
                    )
                ).toStationStatuses()
                cache.setResponseInCacheAndGet(operator, GbfsStandardEnum.station_status, stationsStatusResponse)
                parseResponse<DrammenStationsResponse>(
                    gbfsStandardEnum.getFetchUrl(
                        operator,
                        DRAMMEN_ACCESS_TOKEN
                    )
                ).toStationInformation(
                    cache.getResponseFromCache(
                        Operator.DRAMMENBYSYKKEL,
                        GbfsStandardEnum.station_status
                    ) as GBFSResponse.StationStatusesResponse
                )
            }
            GbfsStandardEnum.station_status -> {
                parseResponse<DrammenStationsStatusResponse>(
                    gbfsStandardEnum.getFetchUrl(
                        operator,
                        DRAMMEN_ACCESS_TOKEN
                    )
                ).toStationStatuses()
            }
            GbfsStandardEnum.system_pricing_plans -> drammenSystemPricingPlan()
        }
        if (response != null) cache.setResponseInCacheAndGet(operator, gbfsStandardEnum, response)
    }
}

fun fetchAndSetDrammenAccessToken() {
    val response = try {
        logger.info("Fetching Drammen access token.")
        parseResponse<DrammenAccessToken>(DRAMMEN_ACCESS_TOKEN_URL)
    } catch (e: Exception) {
        logger.error("Failed to fetch Drammen access token. $e")
        null
    }
    DRAMMEN_ACCESS_TOKEN = response?.access_token ?: ""
}

fun fetch(url: String): String {
    val response = runBlocking { client.get<String>(url) { header("Client-Identifier", "entur-bikeservice") } }
    return response
}
