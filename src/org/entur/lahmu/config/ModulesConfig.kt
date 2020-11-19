package org.entur.lahmu.config

import io.ktor.client.HttpClient
import org.entur.lahmu.controllers.BikesController
import org.entur.lahmu.controllers.BikesControllerImpl
import org.entur.lahmu.controllers.ProviderDiscoveryController
import org.entur.lahmu.controllers.ProviderDiscoveryControllerImpl
import org.entur.lahmu.legacy.service.BikeService
import org.entur.lahmu.legacy.service.BikeServiceImpl
import org.entur.lahmu.legacy.service.Cache
import org.entur.lahmu.legacy.service.InMemoryCache
import org.entur.lahmu.service.ProviderDiscoveryService
import org.entur.lahmu.service.ProviderDiscoveryServiceImpl
import org.koin.dsl.module

val modulesConfig = module {
    single<BikeService> { BikeServiceImpl(HttpClient()) }
    single<Cache> { InMemoryCache(HashMap()) }
    single<BikesController> { (bikeService: BikeService, cache: Cache) -> BikesControllerImpl(bikeService, cache) }

    single<ProviderDiscoveryService> { ProviderDiscoveryServiceImpl() }
    single<ProviderDiscoveryController> { (discoveryService: ProviderDiscoveryService) -> ProviderDiscoveryControllerImpl(discoveryService) }
}
