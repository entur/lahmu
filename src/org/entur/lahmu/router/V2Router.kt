package org.entur.lahmu.router

import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.route
import org.entur.lahmu.controllers.ProviderDiscoveryController
import org.entur.lahmu.service.ProviderDiscoveryService
import org.koin.core.parameter.parametersOf
import org.koin.ktor.ext.inject

fun Routing.v2() {
    val providerDiscoveryService: ProviderDiscoveryService by inject()
    val providerDiscoveryController: ProviderDiscoveryController by inject { parametersOf(providerDiscoveryService) }

    route("v2") {
        get("gbfs") {
            providerDiscoveryController.getServiceDiscovery(this.context)
        }
    }
}
