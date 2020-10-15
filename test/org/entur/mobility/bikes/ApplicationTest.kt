package org.entur.mobility.bikes
import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import java.math.BigDecimal
import org.entur.mobility.bikes.bikeOperators.DrammenAccessToken
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

    private val responseHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
    private val client = HttpClient(MockEngine) {
        engine {
            addHandler { request ->
                when (request.url.toString()) {
                    "https://gbfs.urbansharing.com/oslobysykkel.no/system_information.json" ->
                        respondWithFixture("/oslobysykkelSystemInformation.json")
                    "https://gbfs.urbansharing.com/oslobysykkel.no/station_information.json" ->
                        respondWithFixture("/oslobysykkelStationInformation.json")
                    "https://gbfs.urbansharing.com/oslobysykkel.no/station_status.json" ->
                        respondWithFixture("/oslobysykkelStationStatus.json")
                    "https://sanntidapi-web-prod.azurewebsites.net/api/parkings?type=CityBike" ->
                        respondWithFixture("/kolumbusResponse.json")
                    "https://api.jcdecaux.com/vls/v3/stations?contract=lillestrom&apiKey=null" ->
                        respondWithFixture("/lillestrombysykkelStationsResponse.json")
                    "https://drammen.pub.api.smartbike.com/oauth/v2/token?client_id=null&client_secret=null&grant_type=client_credentials" ->
                        respondWithJson(DrammenAccessToken("test", 3600, "bearer", null))
                    "https://drammen.pub.api.smartbike.com/api/en/v3/stations/status.json?access_token=test" ->
                        respondWithFixture("/drammenStationsStatusResponse.json")
                    "https://drammen.pub.api.smartbike.com/api/en/v3/stations.json?access_token=test" ->
                        respondWithFixture("/drammenStationsResponse.json")
                    else -> error("Unhandled ${request.url}")
                }
            }
        }
    }

    private fun MockRequestHandleScope.respondWithFixture(fixturePath: String): HttpResponseData {
        return respond(
            getTestFixture(fixturePath), headers = responseHeaders
        )
    }

    private fun getTestFixture(resource: String): String {
        return this.javaClass.getResource(resource).readText()
    }

    private fun MockRequestHandleScope.respondWithJson(src: Any): HttpResponseData {
        return respond(Gson().toJson(src), headers = responseHeaders)
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
    fun `get oslobysykkel system information`() = withTestApplication({ routingModule() }) {
        val osloBysykkelSystemInformation = SystemInformation(
            "oslobysykkel",
            "nb",
            "Oslo Bysykkel",
            "UIP Oslo Bysykkel AS",
            "Europe/Oslo",
            "+4791589700",
            "post@oslobysykkel.no"
        )
        with(handleRequest(HttpMethod.Get, "/oslobysykkel/system_information.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals(osloBysykkelSystemInformation, response.content?.let { parseResponse<GBFSResponse.SystemInformationResponse>(it).data })
        }
    }

    @Test
    fun `get oslobysykkel station information`() = withTestApplication({ routingModule() }) {
        val osloBysykkelStationInformation = StationInformation(
            "YOS:VehicleSharingParkingArea:1919",
            "Kværnerveien",
            "Kværnerveien 5",
            BigDecimal("59.90591083488326"),
            BigDecimal("10.778592132296495"),
            6
        )
        with(handleRequest(HttpMethod.Get, "/oslobysykkel/station_information.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals(osloBysykkelStationInformation, response.content?.let { parseResponse<GBFSResponse.StationsInformationResponse>(it).data.stations[0] })
        }
    }

    @Test
    fun `get oslobysykkel station status`() = withTestApplication({ routingModule() }) {
        val osloBysykkelStationStatus = StationStatus(
            "YOS:VehicleSharingParkingArea:1919",
            1,
            1,
            1,
            BigDecimal("1602681289"),
            4,
            2
        )
        with(handleRequest(HttpMethod.Get, "/oslobysykkel/station_status.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals(osloBysykkelStationStatus, response.content?.let { parseResponse<GBFSResponse.StationStatusesResponse>(it).data.stations[0] })
        }
    }

    @Test
    fun `get oslobysykkel system pricing plans`() = withTestApplication({ routingModule() }) {
        with(handleRequest(HttpMethod.Get, "/oslobysykkel/system_pricing_plans.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals("CD863B56-B502-4FDE-B872-C21CD1F8F15C", response.content?.let { parseResponse<GBFSResponse.SystemPricingPlans>(it).plans[0].plan_id })
        }
    }

    @Test
    fun `get kolumbusbysykkel discovery feed`() = withTestApplication({ routingModule() }) {
        with(handleRequest(HttpMethod.Get, "/kolumbusbysykkel/gbfs.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            val discoveryResponse = response.content?.let { parseResponse<GBFSResponse.DiscoveryResponse>(it) }
            assertEquals(15.toLong(), discoveryResponse?.ttl)
            val expected = DiscoveryFeed(
                "system_information",
                "http://localhost:80/kolumbusbysykkel/system_information.json"
            )
            assertEquals(expected, discoveryResponse?.data?.nb?.feeds?.get(0))
        }
    }

    @Test
    fun `get kolumbusbysykkel system information`() = withTestApplication({ routingModule() }) {
        val kolumbusBysykkelSystemInformation = SystemInformation(
            "kolumbusbysykkel",
            "nb",
            "Kolumbus bysykkel",
            null,
            "Europe/Oslo",
            null,
            null
        )

        with(handleRequest(HttpMethod.Get, "/kolumbusbysykkel/system_information.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals(kolumbusBysykkelSystemInformation, response.content?.let { parseResponse<GBFSResponse.SystemInformationResponse>(it).data })
        }
    }

    @Test
    fun `get kolumbusbysykkel station information`() = withTestApplication({ routingModule() }) {
        val kolumbusbysykkelStationInformation = StationInformation(
            "YKO:VehicleSharingParkingArea:66",
            "Sandvika",
            null,
            BigDecimal("58.8708"),
            BigDecimal("5.7657"),
            4
        )
        with(handleRequest(HttpMethod.Get, "/kolumbusbysykkel/station_information.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals(kolumbusbysykkelStationInformation, response.content?.let { parseResponse<GBFSResponse.StationsInformationResponse>(it).data.stations[0] })
        }
    }

    @Test
    fun `get kolumbusbysykkel station status`() = withTestApplication({ routingModule() }) {
        with(handleRequest(HttpMethod.Get, "/kolumbusbysykkel/station_status.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            val stationStatus = response.content?.let { parseResponse<GBFSResponse.StationStatusesResponse>(it).data.stations[0] }
            assertEquals("YKO:VehicleSharingParkingArea:66", stationStatus?.station_id)
            assertEquals(1, stationStatus?.num_bikes_available)
            assertEquals(3, stationStatus?.num_docks_available)
        }
    }

    @Test
    fun `get kolumbusbysykkel system pricing plans`() = withTestApplication({ routingModule() }) {
        with(handleRequest(HttpMethod.Get, "/kolumbusbysykkel/system_pricing_plans.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals("636B0671-ED87-42FB-8FAC-6AE8F3A25826", response.content?.let { parseResponse<GBFSResponse.SystemPricingPlans>(it).plans[0].plan_id })
        }
    }

    @Test
    fun `get lillestrombysykkel discovery feed`() = withTestApplication({ routingModule() }) {
        with(handleRequest(HttpMethod.Get, "/lillestrombysykkel/gbfs.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            val discoveryResponse = response.content?.let { parseResponse<GBFSResponse.DiscoveryResponse>(it) }
            assertEquals(15.toLong(), discoveryResponse?.ttl)
            val expected = DiscoveryFeed(
                "system_information",
                "http://localhost:80/lillestrombysykkel/system_information.json"
            )
            assertEquals(expected, discoveryResponse?.data?.nb?.feeds?.get(0))
        }
    }

    @Test
    fun `get lillestrombysykkel system information`() = withTestApplication({ routingModule() }) {
        val lillestrombysykkelSystemInformation = SystemInformation(
            "lillestrom",
            "nb",
            "Lillestrøm bysykkel",
            null,
            "Europe/Oslo",
            null,
            null
        )

        with(handleRequest(HttpMethod.Get, "/lillestrombysykkel/system_information.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals(lillestrombysykkelSystemInformation, response.content?.let { parseResponse<GBFSResponse.SystemInformationResponse>(it).data })
        }
    }

    @Test
    fun `get lillestrombysykkel station information`() = withTestApplication({ routingModule() }) {
        val lillestrombysykkelStationInformation = StationInformation(
            "YLI:VehicleSharingParkingArea:3",
            "TORVGATA",
            "Torvgata 8, Lillestrøm",
            BigDecimal("59.95585"),
            BigDecimal("11.04745"),
            3
        )
        with(handleRequest(HttpMethod.Get, "/lillestrombysykkel/station_information.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals(lillestrombysykkelStationInformation, response.content?.let { parseResponse<GBFSResponse.StationsInformationResponse>(it).data.stations[0] })
        }
    }

    @Test
    fun `get lillestrombysykkel station status`() = withTestApplication({ routingModule() }) {
        with(handleRequest(HttpMethod.Get, "/lillestrombysykkel/station_status.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            val stationStatus = response.content?.let { parseResponse<GBFSResponse.StationStatusesResponse>(it).data.stations[0] }
            assertEquals("YLI:VehicleSharingParkingArea:3", stationStatus?.station_id)
            assertEquals(12, stationStatus?.num_bikes_available)
            assertEquals(8, stationStatus?.num_docks_available)
        }
    }

    @Test
    fun `get lillestrombysykkel system pricing plans`() = withTestApplication({ routingModule() }) {
        with(handleRequest(HttpMethod.Get, "/lillestrombysykkel/system_pricing_plans.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals("D16E7EC0-47F5-427D-9B71-CD079F989CC6", response.content?.let { parseResponse<GBFSResponse.SystemPricingPlans>(it).plans[0].plan_id })
        }
    }

    @Test
    fun `get drammenbysykkel discovery feed`() = withTestApplication({ routingModule() }) {
        with(handleRequest(HttpMethod.Get, "/drammenbysykkel/gbfs.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            val discoveryResponse = response.content?.let { parseResponse<GBFSResponse.DiscoveryResponse>(it) }
            assertEquals(15.toLong(), discoveryResponse?.ttl)
            val expected = DiscoveryFeed(
                "system_information",
                "http://localhost:80/drammenbysykkel/system_information.json"
            )
            assertEquals(expected, discoveryResponse?.data?.nb?.feeds?.get(0))
        }
    }

    @Test
    fun `get drammenbysykkel system information`() = withTestApplication({ routingModule() }) {
        val drammenbysykkelSystemInformation = SystemInformation(
            "drammen",
            "nb",
            "Drammen Bysykkel",
            null,
            "Europe/Oslo",
            null,
            null
        )

        with(handleRequest(HttpMethod.Get, "/drammenbysykkel/system_information.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals(drammenbysykkelSystemInformation, response.content?.let { parseResponse<GBFSResponse.SystemInformationResponse>(it).data })
        }
    }

    @Test
    fun `get drammenbysykkel station information`() = withTestApplication({ routingModule() }) {
        val drammenbysykkelStationInformation = StationInformation(
            "YDR:VehicleSharingParkingArea:001",
            "Fylkeshuset",
            "Fylkeshuset",
            BigDecimal("59.74819162081"),
            BigDecimal("10.18867093254"),
            12
        )
        with(handleRequest(HttpMethod.Get, "/drammenbysykkel/station_information.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals(drammenbysykkelStationInformation, response.content?.let { parseResponse<GBFSResponse.StationsInformationResponse>(it).data.stations[0] })
        }
    }

    @Test
    fun `get drammenbysykkel station status`() = withTestApplication({ routingModule() }) {
        with(handleRequest(HttpMethod.Get, "/drammenbysykkel/station_status.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            val stationStatus = response.content?.let { parseResponse<GBFSResponse.StationStatusesResponse>(it).data.stations[0] }
            assertEquals("YDR:VehicleSharingParkingArea:001", stationStatus?.station_id)
            assertEquals(0, stationStatus?.num_bikes_available)
            assertEquals(12, stationStatus?.num_docks_available)
        }
    }

    @Test
    fun `get drammenbysykkel system pricing plans`() = withTestApplication({ routingModule() }) {
        with(handleRequest(HttpMethod.Get, "/drammenbysykkel/system_pricing_plans.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals("8B00A621-82E8-4AC0-9B89-ABEAF99BD238", response.content?.let { parseResponse<GBFSResponse.SystemPricingPlans>(it).plans[0].plan_id })
        }
    }
}