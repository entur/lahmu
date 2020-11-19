package org.entur.lahmu.config

import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.metrics.micrometer.MicrometerMetrics
import io.ktor.response.header
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.BaseApplicationEngine
import io.ktor.server.engine.EngineAPI
import io.ktor.server.engine.embeddedServer
import io.ktor.server.jetty.Jetty
import io.ktor.util.KtorExperimentalAPI
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import java.util.Timer
import java.util.UUID
import kotlin.concurrent.schedule
import kotlin.concurrent.thread
import kotlinx.coroutines.runBlocking
import org.entur.lahmu.legacy.service.BikeService
import org.entur.lahmu.legacy.service.Cache
import org.entur.lahmu.router.bikes
import org.entur.lahmu.router.v2
import org.entur.lahmu.util.sanitize
import org.koin.ktor.ext.Koin
import org.koin.ktor.ext.inject
import org.slf4j.MDC

@KtorExperimentalAPI
@EngineAPI
fun setup(): BaseApplicationEngine {
    return embeddedServer(Jetty, watchPaths = listOf("lahmu"), port = 8080, module = Application::mainModule)
}

fun Application.mainModule() {
    val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val bikeService: BikeService by inject()
    val cache: Cache by inject()

    install(Koin) {
        modules(modulesConfig)
    }

    install(MicrometerMetrics) {
        registry = meterRegistry
    }

    thread(start = true) {
        Timer().schedule(0L, TIME_TO_LIVE_DRAMMEN_ACCESS_KEY_MS) {
            bikeService.fetchAndSetDrammenAccessToken()
        }
        Timer().schedule(0L, POLL_INTERVAL_MS) {
            bikeService.poll(cache)
        }
    }

    routing {
        get("/actuator/prometheus") {
            call.respondText(meterRegistry.scrape())
        }

        get("/health") {
            call.respondText("OK")
        }

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
    }

    bikesRoutingModule()
    v2RoutingModule()
}

fun Application.bikesRoutingModule() {
    routing {
        bikes()
    }
}

fun Application.v2RoutingModule() {
    routing {
        v2()
    }
}
