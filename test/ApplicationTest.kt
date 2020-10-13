package com.entur.test
import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.entur.mobility.bikes.BikeService
import org.entur.mobility.bikes.BikeServiceImpl
import org.entur.mobility.bikes.Cache
import org.entur.mobility.bikes.DiscoveryFeed
import org.entur.mobility.bikes.GBFSResponse
import org.entur.mobility.bikes.InMemoryCache
import org.entur.mobility.bikes.SystemInformation
import org.entur.mobility.bikes.parseResponse
import org.entur.mobility.bikes.routingModule
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.KoinTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApplicationTest : KoinTest {

    private val osloBysykkelSystemInformation = SystemInformation(
        "oslobysykkel",
        "nb",
        "Oslo Bysykkel",
        "UIP Oslo Bysykkel AS",
        "Europe/Oslo",
        "+4791589700",
        "post@oslobysykkel.no"
    )

    private val osloBysykkelSystemInformationResponse = GBFSResponse.SystemInformationResponse(
        LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
        10,
        osloBysykkelSystemInformation
    )

    private val client = HttpClient(MockEngine) {
        engine {
            addHandler { request ->
                when (request.url.toString()) {
                    "https://gbfs.urbansharing.com/oslobysykkel.no/system_information.json" -> {
                        val responseHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                        respond(Gson().toJson(osloBysykkelSystemInformationResponse).toString(), headers = responseHeaders)
                    }
                    else -> error("Unhandled ${request.url}")
                }
            }
        }
    }

    private val mockedAppModule: Module = module(override = true) {
        single<BikeService> { BikeServiceImpl(client) }
        single<Cache> { InMemoryCache(HashMap()) }
    }

    @BeforeEach
    fun setup() {
        stopKoin()
        startKoin { modules(mockedAppModule) }
    }

    @AfterAll
    fun cleanup() {
        stopKoin()
    }

    @Test
    fun `health endpoint returns "OK"`() = withTestApplication({ routingModule() }) {
        with(handleRequest(HttpMethod.Get, "/health")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals("OK", response.content)
        }
    }

    @Test
    fun `get oslobysykkel discovery feed`() = withTestApplication({ routingModule() }) {
        with(handleRequest(HttpMethod.Get, "/oslobysykkel/gbfs.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            val discoveryResponse = response.content?.let { parseResponse<GBFSResponse.DiscoveryResponse>(it) }
            assertEquals(15.toLong(), discoveryResponse?.ttl)
            val expected = DiscoveryFeed(
                "system_information",
                "http://localhost:80/oslobysykkel/system_information.json"
            )
            assertEquals(expected, discoveryResponse?.data?.nb?.feeds?.get(0))
        }
    }

    @Test
    fun `get oslobysykkel system information feed`() = withTestApplication({ routingModule() }) {
        with(handleRequest(HttpMethod.Get, "/oslobysykkel/system_information.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            val systemInformationResponse = response.content?.let { parseResponse<GBFSResponse.SystemInformationResponse>(it) }
            assertEquals(10.toLong(), systemInformationResponse?.ttl)
            val expected = SystemInformation(
                "oslobysykkel",
                "nb",
                "Oslo Bysykkel",
                "UIP Oslo Bysykkel AS",
                "Europe/Oslo",
                "+4791589700",
                "post@oslobysykkel.no"
            )
            assertEquals(expected, systemInformationResponse?.data)
        }
    }
}
