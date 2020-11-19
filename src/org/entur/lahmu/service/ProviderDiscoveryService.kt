package org.entur.lahmu.service

import org.entur.lahmu.domain.ProviderDiscovery

interface ProviderDiscoveryService {
    fun get(): ProviderDiscovery
}
