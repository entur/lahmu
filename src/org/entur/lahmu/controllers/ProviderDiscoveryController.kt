package org.entur.lahmu.controllers

import io.ktor.application.ApplicationCall

interface ProviderDiscoveryController {
    suspend fun getServiceDiscovery(call: ApplicationCall)
}
