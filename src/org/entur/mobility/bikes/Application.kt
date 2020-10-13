package org.entur.mobility.bikes

import com.google.gson.Gson
import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
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
import kotlinx.coroutines.runBlocking
import org.entur.mobility.bikes.bikeOperators.Operator
import org.entur.mobility.bikes.bikeOperators.getOperatorsWithDiscovery
import org.koin.dsl.module
import org.koin.ktor.ext.Koin
import org.koin.ktor.ext.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC

val logger: Logger = LoggerFactory.getLogger("org.entur.mobility.bikes")

val appModule = module {
    single<BikeService> { BikeServiceImpl(HttpClient()) }
    single<Cache> { InMemoryCache(HashMap()) }
}

fun main() {
    val server = embeddedServer(Jetty, watchPaths = listOf("bikeservice"), port = 8080, module = Application::main)
    server.start(wait = true)
}

fun Application.main() {
    val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val bikeService: BikeService by inject()
    val cache: Cache by inject()

    thread(start = true) {
        Timer().schedule(0L, TIME_TO_LIVE_DRAMMEN_ACCESS_KEY_MS) {
            bikeService.fetchAndSetDrammenAccessToken()
        }
        Timer().schedule(0L, POLL_INTERVAL_MS) {
            bikeService.poll(cache)
        }
    }

    install(Koin) {
        modules(appModule)
    }

    install(MicrometerMetrics) {
        registry = meterRegistry
    }

    routing {
        get("/actuator/prometheus") {
            call.respondText(meterRegistry.scrape())
        }
    }

    routingModule()
}

fun Application.routingModule() {
    val bikeService: BikeService by inject()
    val cache: Cache by inject()

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
                    bikeService.fetchAndStoreInCache(
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
