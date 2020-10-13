package org.entur.mobility.bikes

import com.google.gson.reflect.TypeToken
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import java.lang.Exception
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.entur.mobility.bikes.GbfsStandardEnum.Companion.getFetchUrl
import org.entur.mobility.bikes.bikeOperators.DrammenAccessToken
import org.entur.mobility.bikes.bikeOperators.DrammenStationsResponse
import org.entur.mobility.bikes.bikeOperators.DrammenStationsStatusResponse
import org.entur.mobility.bikes.bikeOperators.JCDecauxResponse
import org.entur.mobility.bikes.bikeOperators.JCDecauxStation
import org.entur.mobility.bikes.bikeOperators.KolumbusResponse
import org.entur.mobility.bikes.bikeOperators.KolumbusStation
import org.entur.mobility.bikes.bikeOperators.Operator
import org.entur.mobility.bikes.bikeOperators.Operator.Companion.isDrammenSmartBike
import org.entur.mobility.bikes.bikeOperators.Operator.Companion.isJCDecaux
import org.entur.mobility.bikes.bikeOperators.Operator.Companion.isKolumbus
import org.entur.mobility.bikes.bikeOperators.Operator.Companion.isUrbanSharing
import org.entur.mobility.bikes.bikeOperators.drammenSystemInformation
import org.entur.mobility.bikes.bikeOperators.drammenSystemPricingPlan
import org.entur.mobility.bikes.bikeOperators.jcDecauxSystemInformation
import org.entur.mobility.bikes.bikeOperators.jcDecauxSystemPricingPlans
import org.entur.mobility.bikes.bikeOperators.kolumbusBysykkelURL
import org.entur.mobility.bikes.bikeOperators.kolumbusSystemPricingPlans
import org.entur.mobility.bikes.bikeOperators.lillestromBysykkelURL
import org.entur.mobility.bikes.bikeOperators.toStationInformation
import org.entur.mobility.bikes.bikeOperators.toStationStatus
import org.entur.mobility.bikes.bikeOperators.toStationStatuses
import org.entur.mobility.bikes.bikeOperators.urbanSharingSystemPricePlan

interface BikeService {
    val client: HttpClient

    fun poll(cache: Cache)
    fun fetchAndStoreInCache(
        cache: Cache,
        operator: Operator,
        gbfsStandardEnum: GbfsStandardEnum
    )
    fun fetchAndSetDrammenAccessToken()
}

class BikeServiceImpl(override val client: HttpClient) : BikeService {
    var DRAMMEN_ACCESS_TOKEN = ""

    override fun poll(cache: Cache) {
        Operator.values().forEach { operator ->
            GlobalScope.async {
                logger.info("Polling $operator")
                try {
                    if (operator.isUrbanSharing()) {
                        GbfsStandardEnum.values().forEach { gbfsEnum ->
                            fetchAndStoreInCache(
                                cache = cache,
                                operator = operator,
                                gbfsStandardEnum = gbfsEnum
                            )
                        }
                    } else if (operator.isDrammenSmartBike()) {
                        fetchAndStoreInCache(cache, operator, GbfsStandardEnum.system_information)
                        fetchAndStoreInCache(cache, operator, GbfsStandardEnum.station_information)
                    } else {
                        fetchAndStoreInCache(cache, operator, GbfsStandardEnum.gbfs)
                    }
                } catch (e: Exception) {
                    logger.error("Failed to poll from $operator. $e")
                }
            }
        }
    }
    override fun fetchAndStoreInCache(
        cache: Cache,
        operator: Operator,
        gbfsStandardEnum: GbfsStandardEnum
    ) {
        if (operator.isUrbanSharing()) {
            val response = when (gbfsStandardEnum) {
                GbfsStandardEnum.gbfs -> {
                    null
                }
                GbfsStandardEnum.system_information -> {
                    parseResponse<GBFSResponse.SystemInformationResponse>(fetch(gbfsStandardEnum.getFetchUrl(operator)))
                }
                GbfsStandardEnum.station_information -> {
                    parseResponse<GBFSResponse.StationsInformationResponse>(fetch(gbfsStandardEnum.getFetchUrl(operator))).toNeTEx(
                        operator
                    )
                }
                GbfsStandardEnum.station_status -> {
                    parseResponse<GBFSResponse.StationStatusesResponse>(fetch(gbfsStandardEnum.getFetchUrl(operator))).toNeTEx(
                        operator
                    )
                }
                GbfsStandardEnum.system_pricing_plans -> urbanSharingSystemPricePlan(operator)
                GbfsStandardEnum.free_bike_status -> {
                    null
                }
            }
            if (response != null) cache.setResponseInCacheAndGet(operator, gbfsStandardEnum, response)
        } else if (operator.isKolumbus()) {
            val response = KolumbusResponse(parseKolumbusResponse())
            cache.setResponseInCacheAndGet(
                operator,
                GbfsStandardEnum.system_information,
                response.jcDecauxSystemInformation()
            )
            cache.setResponseInCacheAndGet(
                operator,
                GbfsStandardEnum.station_information,
                response.toStationInformation()
            )
            cache.setResponseInCacheAndGet(
                operator,
                GbfsStandardEnum.station_status,
                response.toStationStatus()
            )
            cache.setResponseInCacheAndGet(
                operator,
                GbfsStandardEnum.system_pricing_plans,
                kolumbusSystemPricingPlans()
            )
        } else if (operator.isJCDecaux()) {
            cache.setResponseInCacheAndGet(
                operator,
                GbfsStandardEnum.system_pricing_plans,
                jcDecauxSystemPricingPlans()
            )
            cache.setResponseInCacheAndGet(
                operator,
                GbfsStandardEnum.system_information,
                jcDecauxSystemInformation()
            )
            val response = JCDecauxResponse(data = parseJCDecauxResponse())
            cache.setResponseInCacheAndGet(
                operator,
                GbfsStandardEnum.station_status,
                response.toStationStatus()
            )
            cache.setResponseInCacheAndGet(
                operator,
                GbfsStandardEnum.station_information,
                response.toStationInformation()
            )
        } else if (operator.isDrammenSmartBike()) {
            if (DRAMMEN_ACCESS_TOKEN == "") fetchAndSetDrammenAccessToken()
            val response = when (gbfsStandardEnum) {
                GbfsStandardEnum.gbfs -> {
                    null
                }
                GbfsStandardEnum.system_information -> {
                    drammenSystemInformation()
                }
                GbfsStandardEnum.station_information -> {
                    val stationsStatusResponse = parseResponse<DrammenStationsStatusResponse>(fetch(
                        GbfsStandardEnum.station_status.getFetchUrl(
                            operator,
                            DRAMMEN_ACCESS_TOKEN
                        )
                    )).toStationStatuses()
                    cache.setResponseInCacheAndGet(operator, GbfsStandardEnum.station_status, stationsStatusResponse)
                    parseResponse<DrammenStationsResponse>(fetch(
                        gbfsStandardEnum.getFetchUrl(
                            operator,
                            DRAMMEN_ACCESS_TOKEN
                        )
                    )).toStationInformation(
                        cache.getResponseFromCache(
                            Operator.DRAMMENBYSYKKEL,
                            GbfsStandardEnum.station_status
                        ) as GBFSResponse.StationStatusesResponse
                    )
                }
                GbfsStandardEnum.station_status -> {
                    parseResponse<DrammenStationsStatusResponse>(fetch(
                        gbfsStandardEnum.getFetchUrl(
                            operator,
                            DRAMMEN_ACCESS_TOKEN
                        )
                    )).toStationStatuses()
                }
                GbfsStandardEnum.system_pricing_plans -> drammenSystemPricingPlan()
                GbfsStandardEnum.free_bike_status -> {
                    null
                }
            }
            if (response != null) cache.setResponseInCacheAndGet(operator, gbfsStandardEnum, response)
        }
    }

    override fun fetchAndSetDrammenAccessToken() {
        val response = try {
            logger.info("Fetching Drammen access token.")
            parseResponse<DrammenAccessToken>(fetch(DRAMMEN_ACCESS_TOKEN_URL))
        } catch (e: Exception) {
            logger.error("Failed to fetch Drammen access token. $e")
            null
        }
        DRAMMEN_ACCESS_TOKEN = response?.access_token ?: ""
    }

    private fun parseKolumbusResponse(): List<KolumbusStation> {
        val response = fetch(kolumbusBysykkelURL.getValue(GbfsStandardEnum.system_information))
        val itemType = object : TypeToken<List<KolumbusStation>>() {}.type
        return parseResponse(response, itemType)
    }

    private fun parseJCDecauxResponse(): List<JCDecauxStation> {
        val response = fetch(lillestromBysykkelURL.getValue(GbfsStandardEnum.system_information))
        val itemType = object : TypeToken<List<JCDecauxStation>>() {}.type
        return parseResponse(response, itemType)
    }

    private fun fetch(url: String): String {
        val response = runBlocking { client.get<String>(url) { header("Client-Identifier", "entur-bikeservice") } }
        return response
    }
}
