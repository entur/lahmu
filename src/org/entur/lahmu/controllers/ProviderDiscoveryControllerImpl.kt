package org.entur.lahmu.controllers

import io.ktor.application.ApplicationCall
import io.ktor.http.ContentType
import io.ktor.response.respondText
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.entur.lahmu.service.ProviderDiscoveryService

class ProviderDiscoveryControllerImpl(private val discoveryService: ProviderDiscoveryService) : ProviderDiscoveryController {
    override suspend fun getServiceDiscovery(call: ApplicationCall) {
        val discoveryService = discoveryService.get()
        call.respondText(Json.encodeToString(discoveryService), ContentType.Application.Json)
    }
}
