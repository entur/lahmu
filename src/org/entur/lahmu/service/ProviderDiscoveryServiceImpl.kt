package org.entur.lahmu.service

import org.entur.lahmu.domain.ProviderDiscovery

class ProviderDiscoveryServiceImpl : ProviderDiscoveryService {
    override fun get(): ProviderDiscovery {
        return ProviderDiscovery(
            lastUpdated = System.currentTimeMillis() / 1000,
            ttl = 300,
            data = ProviderDiscovery.ProviderDiscoveryData(
                providers = listOf()
            )
        )
    }
}
