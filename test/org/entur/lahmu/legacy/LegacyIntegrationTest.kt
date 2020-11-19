package org.entur.lahmu.legacy
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import java.math.BigDecimal
import org.entur.lahmu.config.bikesRoutingModule
import org.entur.lahmu.controllers.BikesController
import org.entur.lahmu.controllers.BikesControllerImpl
import org.entur.lahmu.legacy.service.BikeService
import org.entur.lahmu.legacy.service.BikeServiceImpl
import org.entur.lahmu.legacy.service.Cache
import org.entur.lahmu.legacy.service.InMemoryCache
import org.entur.lahmu.util.parseResponse
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
class LegacyIntegrationTest : KoinTest {

    private val mockedAppModule: Module = module(override = true) {
        single<BikeService> { BikeServiceImpl(HttpMockEngine().client) }
        single<Cache> { InMemoryCache(HashMap()) }
        single<BikesController> { (bikeService: BikeService, cache: Cache) -> BikesControllerImpl(bikeService, cache) }
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
    fun `get discovery feed for unknown operator`() = withTestApplication({ bikesRoutingModule() }) {
        with(handleRequest(HttpMethod.Get, "/bikes/foobar/gbfs.json")) {
            assertEquals(HttpStatusCode.NotFound, response.status())
        }
    }

    @Test
    fun `get system information feed for unknown operator`() = withTestApplication({ bikesRoutingModule() }) {
        with(handleRequest(HttpMethod.Get, "/bikes/foobar/system_information.json")) {
            assertEquals(HttpStatusCode.NotFound, response.status())
        }
    }

    @Test
    fun `get unknown gbfs feed`() = withTestApplication({ bikesRoutingModule() }) {
        with(handleRequest(HttpMethod.Get, "/bikes/oslobysykkel/foo_bar.json")) {
            assertEquals(HttpStatusCode.NotFound, response.status())
        }
    }

    @Test
    fun `get oslobysykkel discovery feed`() = withTestApplication({ bikesRoutingModule() }) {
        with(handleRequest(HttpMethod.Get, "/bikes/oslobysykkel/gbfs.json")) {
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
    fun `get oslobysykkel system information`() = withTestApplication({ bikesRoutingModule() }) {
        val osloBysykkelSystemInformation = SystemInformation(
            "oslobysykkel",
            "nb",
            "Oslo Bysykkel",
            "UIP Oslo Bysykkel AS",
            "Europe/Oslo",
            "+4791589700",
            "post@oslobysykkel.no"
        )
        with(handleRequest(HttpMethod.Get, "/bikes/oslobysykkel/system_information.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals(osloBysykkelSystemInformation, response.content?.let { parseResponse<GBFSResponse.SystemInformationResponse>(it).data })
        }
    }

    @Test
    fun `get oslobysykkel station information`() = withTestApplication({ bikesRoutingModule() }) {
        val osloBysykkelStationInformation = StationInformation(
            "YOS:VehicleSharingParkingArea:1919",
            "Kværnerveien",
            "Kværnerveien 5",
            BigDecimal("59.90591083488326"),
            BigDecimal("10.778592132296495"),
            6
        )
        with(handleRequest(HttpMethod.Get, "/bikes/oslobysykkel/station_information.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals(osloBysykkelStationInformation, response.content?.let { parseResponse<GBFSResponse.StationsInformationResponse>(it).data.stations[0] })
        }
    }

    @Test
    fun `get oslobysykkel station status`() = withTestApplication({ bikesRoutingModule() }) {
        val osloBysykkelStationStatus = StationStatus(
            "YOS:VehicleSharingParkingArea:1919",
            1,
            1,
            1,
            BigDecimal("1602681289"),
            4,
            2
        )
        with(handleRequest(HttpMethod.Get, "/bikes/oslobysykkel/station_status.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals(osloBysykkelStationStatus, response.content?.let { parseResponse<GBFSResponse.StationStatusesResponse>(it).data.stations[0] })
        }
    }

    @Test
    fun `get oslobysykkel system pricing plans`() = withTestApplication({ bikesRoutingModule() }) {
        with(handleRequest(HttpMethod.Get, "/bikes/oslobysykkel/system_pricing_plans.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals("CD863B56-B502-4FDE-B872-C21CD1F8F15C", response.content?.let { parseResponse<GBFSResponse.SystemPricingPlans>(it).plans[0].planId })
        }
    }

    @Test
    fun `get oslobysykkel free bike status not found`() = withTestApplication({ bikesRoutingModule() }) {
        with(handleRequest(HttpMethod.Get, "/bikes/oslobysykkel/free_bike_status.json")) {
            assertEquals(HttpStatusCode.NotFound, response.status())
        }
    }

    @Test
    fun `get kolumbusbysykkel discovery feed`() = withTestApplication({ bikesRoutingModule() }) {
        with(handleRequest(HttpMethod.Get, "/bikes/kolumbusbysykkel/gbfs.json")) {
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
    fun `get kolumbusbysykkel system information`() = withTestApplication({ bikesRoutingModule() }) {
        val kolumbusBysykkelSystemInformation = SystemInformation(
            "kolumbusbysykkel",
            "nb",
            "Kolumbus bysykkel",
            null,
            "Europe/Oslo",
            null,
            null
        )

        with(handleRequest(HttpMethod.Get, "/bikes/kolumbusbysykkel/system_information.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals(kolumbusBysykkelSystemInformation, response.content?.let { parseResponse<GBFSResponse.SystemInformationResponse>(it).data })
        }
    }

    @Test
    fun `get kolumbusbysykkel station information`() = withTestApplication({ bikesRoutingModule() }) {
        val kolumbusbysykkelStationInformation = StationInformation(
            "YKO:VehicleSharingParkingArea:66",
            "Sandvika",
            null,
            BigDecimal("58.8708"),
            BigDecimal("5.7657"),
            4
        )
        with(handleRequest(HttpMethod.Get, "/bikes/kolumbusbysykkel/station_information.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals(kolumbusbysykkelStationInformation, response.content?.let { parseResponse<GBFSResponse.StationsInformationResponse>(it).data.stations[0] })
        }
    }

    @Test
    fun `get kolumbusbysykkel station status`() = withTestApplication({ bikesRoutingModule() }) {
        with(handleRequest(HttpMethod.Get, "/bikes/kolumbusbysykkel/station_status.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            val stationStatus = response.content?.let { parseResponse<GBFSResponse.StationStatusesResponse>(it).data.stations[0] }
            assertEquals("YKO:VehicleSharingParkingArea:66", stationStatus?.stationId)
            assertEquals(1, stationStatus?.numBikesAvailable)
            assertEquals(3, stationStatus?.numDocksAvailable)
        }
    }

    @Test
    fun `get kolumbusbysykkel system pricing plans`() = withTestApplication({ bikesRoutingModule() }) {
        with(handleRequest(HttpMethod.Get, "/bikes/kolumbusbysykkel/system_pricing_plans.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals("636B0671-ED87-42FB-8FAC-6AE8F3A25826", response.content?.let { parseResponse<GBFSResponse.SystemPricingPlans>(it).plans[0].planId })
        }
    }

    @Test
    fun `get lillestrombysykkel discovery feed`() = withTestApplication({ bikesRoutingModule() }) {
        with(handleRequest(HttpMethod.Get, "/bikes/lillestrombysykkel/gbfs.json")) {
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
    fun `get lillestrombysykkel system information`() = withTestApplication({ bikesRoutingModule() }) {
        val lillestrombysykkelSystemInformation = SystemInformation(
            "lillestrom",
            "nb",
            "Lillestrøm bysykkel",
            null,
            "Europe/Oslo",
            null,
            null
        )

        with(handleRequest(HttpMethod.Get, "/bikes/lillestrombysykkel/system_information.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals(lillestrombysykkelSystemInformation, response.content?.let { parseResponse<GBFSResponse.SystemInformationResponse>(it).data })
        }
    }

    @Test
    fun `get lillestrombysykkel station information`() = withTestApplication({ bikesRoutingModule() }) {
        val lillestrombysykkelStationInformation = StationInformation(
            "YLI:VehicleSharingParkingArea:3",
            "TORVGATA",
            "Torvgata 8, Lillestrøm",
            BigDecimal("59.95585"),
            BigDecimal("11.04745"),
            3
        )
        with(handleRequest(HttpMethod.Get, "/bikes/lillestrombysykkel/station_information.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals(lillestrombysykkelStationInformation, response.content?.let { parseResponse<GBFSResponse.StationsInformationResponse>(it).data.stations[0] })
        }
    }

    @Test
    fun `get lillestrombysykkel station status`() = withTestApplication({ bikesRoutingModule() }) {
        with(handleRequest(HttpMethod.Get, "/bikes/lillestrombysykkel/station_status.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            val stationStatus = response.content?.let { parseResponse<GBFSResponse.StationStatusesResponse>(it).data.stations[0] }
            assertEquals("YLI:VehicleSharingParkingArea:3", stationStatus?.stationId)
            assertEquals(12, stationStatus?.numBikesAvailable)
            assertEquals(8, stationStatus?.numDocksAvailable)
        }
    }

    @Test
    fun `get lillestrombysykkel system pricing plans`() = withTestApplication({ bikesRoutingModule() }) {
        with(handleRequest(HttpMethod.Get, "/bikes/lillestrombysykkel/system_pricing_plans.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals("D16E7EC0-47F5-427D-9B71-CD079F989CC6", response.content?.let { parseResponse<GBFSResponse.SystemPricingPlans>(it).plans[0].planId })
        }
    }

    @Test
    fun `get drammenbysykkel discovery feed`() = withTestApplication({ bikesRoutingModule() }) {
        with(handleRequest(HttpMethod.Get, "/bikes/drammenbysykkel/gbfs.json")) {
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
    fun `get drammenbysykkel system information`() = withTestApplication({ bikesRoutingModule() }) {
        val drammenbysykkelSystemInformation = SystemInformation(
            "drammen",
            "nb",
            "Drammen Bysykkel",
            null,
            "Europe/Oslo",
            null,
            null
        )

        with(handleRequest(HttpMethod.Get, "/bikes/drammenbysykkel/system_information.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals(drammenbysykkelSystemInformation, response.content?.let { parseResponse<GBFSResponse.SystemInformationResponse>(it).data })
        }
    }

    @Test
    fun `get drammenbysykkel station information`() = withTestApplication({ bikesRoutingModule() }) {
        val drammenbysykkelStationInformation = StationInformation(
            "YDR:VehicleSharingParkingArea:001",
            "Fylkeshuset",
            "Fylkeshuset",
            BigDecimal("59.74819162081"),
            BigDecimal("10.18867093254"),
            12
        )
        with(handleRequest(HttpMethod.Get, "/bikes/drammenbysykkel/station_information.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals(drammenbysykkelStationInformation, response.content?.let { parseResponse<GBFSResponse.StationsInformationResponse>(it).data.stations[0] })
        }
    }

    @Test
    fun `get drammenbysykkel station status`() = withTestApplication({ bikesRoutingModule() }) {
        with(handleRequest(HttpMethod.Get, "/bikes/drammenbysykkel/station_status.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            val stationStatus = response.content?.let { parseResponse<GBFSResponse.StationStatusesResponse>(it).data.stations[0] }
            assertEquals("YDR:VehicleSharingParkingArea:001", stationStatus?.stationId)
            assertEquals(0, stationStatus?.numBikesAvailable)
            assertEquals(12, stationStatus?.numDocksAvailable)
        }
    }

    @Test
    fun `get drammenbysykkel system pricing plans`() = withTestApplication({ bikesRoutingModule() }) {
        with(handleRequest(HttpMethod.Get, "/bikes/drammenbysykkel/system_pricing_plans.json")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals("8B00A621-82E8-4AC0-9B89-ABEAF99BD238", response.content?.let { parseResponse<GBFSResponse.SystemPricingPlans>(it).plans[0].planId })
        }
    }
}
