package org.entur.lahmu.controllers

import com.google.gson.Gson
import io.ktor.application.ApplicationCall
import io.ktor.features.NotFoundException
import io.ktor.http.ContentType
import io.ktor.request.host
import io.ktor.request.port
import io.ktor.response.respondText
import java.lang.Exception
import org.entur.lahmu.legacy.GbfsStandardEnum
import org.entur.lahmu.legacy.bikeOperators.Operator
import org.entur.lahmu.legacy.bikeOperators.getOperatorsWithDiscovery
import org.entur.lahmu.legacy.getDiscovery
import org.entur.lahmu.legacy.getGbfsEndpoint
import org.entur.lahmu.legacy.service.BikeService
import org.entur.lahmu.legacy.service.Cache
import org.entur.lahmu.logger

interface BikesController {
    suspend fun getServiceDirectory(call: ApplicationCall)
    suspend fun getDiscoveryFeed(call: ApplicationCall)
    suspend fun getGbfsFeed(call: ApplicationCall)
}

class BikesControllerImpl(private val bikeService: BikeService, private val cache: Cache) : BikesController {
    override suspend fun getServiceDirectory(call: ApplicationCall) {
        val host = call.request.host()
        val port = call.request.port()

        call.respondText(Gson().toJson(getOperatorsWithDiscovery(host, port)), ContentType.Application.Json)
    }

    override suspend fun getDiscoveryFeed(call: ApplicationCall) {
        val operator = getOperator(call) ?: throw NotFoundException()
        val gbfsEndpoints = getGbfsEndpoint(operator, call.request.host(), call.request.port())
        val response = getDiscovery(gbfsEndpoints)

        call.respondText(Gson().toJson(response), ContentType.Application.Json)
    }

    override suspend fun getGbfsFeed(call: ApplicationCall) {
        val operator = getOperator(call) ?: throw NotFoundException()
        val gbfsEnum = getGbfsEnum(call) ?: throw NotFoundException()
        if (!cache.isValidCache(operator, gbfsEnum)) {
            try {
                bikeService.fetchAndStoreInCache(
                    cache = cache,
                    operator = operator,
                    gbfsStandardEnum = gbfsEnum
                )
            } catch (e: Exception) {
                logger.error("Failed to fetch $gbfsEnum from $operator. $e")
            }
        }
        val result = cache.getResponseFromCache(operator, gbfsEnum) ?: throw NotFoundException()
        call.respondText(Gson().toJson(result), ContentType.Application.Json)
    }

    private fun getOperator(call: ApplicationCall): Operator? {
        return enumValueOfOrNull<Operator>(call.parameters["operator"]?.toUpperCase())
    }

    private fun getGbfsEnum(call: ApplicationCall): GbfsStandardEnum? {
        return enumValueOfOrNull<GbfsStandardEnum>(call.parameters["service"]?.toLowerCase())
    }

    private inline fun <reified T : Enum<T>> enumValueOfOrNull(name: String?): T? {
        return enumValues<T>().find { it.name == name }
    }
}
