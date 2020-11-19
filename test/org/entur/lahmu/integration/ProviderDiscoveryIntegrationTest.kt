package org.entur.lahmu.integration

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.entur.lahmu.config.v2RoutingModule
import org.entur.lahmu.controllers.ProviderDiscoveryController
import org.entur.lahmu.controllers.ProviderDiscoveryControllerImpl
import org.entur.lahmu.domain.ProviderDiscovery
import org.entur.lahmu.service.ProviderDiscoveryService
import org.entur.lahmu.service.ProviderDiscoveryServiceImpl
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.KoinTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProviderDiscoveryIntegrationTest : KoinTest {
    private val mockedAppModule: Module = module(override = true) {
        single<ProviderDiscoveryService> { ProviderDiscoveryServiceImpl() }
        single<ProviderDiscoveryController> { (providerDiscoveryService: ProviderDiscoveryService) -> ProviderDiscoveryControllerImpl(providerDiscoveryService) }
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
    fun `get provider discovery`() = withTestApplication({ v2RoutingModule() }) {
        with(handleRequest(HttpMethod.Get, "/v2/gbfs")) {
            Assertions.assertEquals(HttpStatusCode.OK, response.status())
            val providerDiscovery = response.content?.let { Json.decodeFromString<ProviderDiscovery>(it) }
            Assertions.assertEquals(300, providerDiscovery?.ttl)
        }
    }
}
