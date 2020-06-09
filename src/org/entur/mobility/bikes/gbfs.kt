package org.entur.mobility.bikes

import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.entur.mobility.bikes.bikeOperators.Operator
import org.entur.mobility.bikes.bikeOperators.Operator.Companion.getFetchUrls
import org.entur.mobility.bikes.bikeOperators.mapIdToNeTEx

enum class GbfsStandardEnum {
    gbfs,
    system_information,
    station_information,
    station_status,
    system_pricing_plans;

    companion object {
        fun GbfsStandardEnum.getFetchUrl(operator: Operator, accessToken: String = ""): String =
            operator.getFetchUrls(accessToken).getValue(this)
        }
    }

sealed class GBFSResponse(
    val last_updated: Long,
    val ttl: Long
) {
    class DiscoveryResponse(last_updated: Long, ttl: Long, val data: Discovery) :
        GBFSResponse(last_updated, ttl)

    class SystemInformationResponse(last_updated: Long, ttl: Long, val data: SystemInformation) :
        GBFSResponse(last_updated, ttl)

    class StationsInformationResponse(last_updated: Long, ttl: Long, val data: StationsInformation) :
        GBFSResponse(last_updated, ttl) {
        fun toNeTEx(operator: Operator): StationsInformationResponse =
            StationsInformationResponse(
                last_updated = last_updated,
                ttl = ttl,
                data = this.data.toNeTEx(operator)
            )
    }

    class StationStatusesResponse(last_updated: Long, ttl: Long, val data: StationStatuses) :
        GBFSResponse(last_updated, ttl) {
        fun toNeTEx(operator: Operator) = StationStatusesResponse(
            last_updated = last_updated,
            ttl = ttl,
            data = data.toNeTEx(operator)
        )
    }

    class SystemPricingPlans(last_updated: Long, ttl: Long, val plans: List<SystemPricePlan>) : GBFSResponse(last_updated, ttl)
}

data class Discovery(val nb: DiscoveryLanguage)
data class DiscoveryLanguage(val feeds: List<DiscoveryFeed>)
data class DiscoveryFeed(val name: String, val url: String)

data class SystemInformation(
    val system_id: String,
    val language: String,
    val name: String,
    val operator: String?,
    val timezone: String,
    val phone_number: String?,
    val email: String?
)

data class StationsInformation(val stations: List<StationInformation>)
data class StationInformation(
    val station_id: String,
    val name: String,
    val address: String?,
    val lat: BigDecimal,
    val lon: BigDecimal,
    val capacity: Int
)

data class StationStatuses(val stations: List<StationStatus>)
data class StationStatus(
    val station_id: String,
    val is_installed: Int,
    val is_renting: Int,
    val is_returning: Int,
    val last_reported: BigDecimal,
    val num_bikes_available: Int,
    val num_docks_available: Int
)

data class SystemPricePlan(
    val plan_id: String,
    val url: String?,
    val name: String,
    val currency: String,
    val price: Double,
    val is_taxable: Int,
    val description: String
)

fun StationsInformation.toNeTEx(operator: Operator): StationsInformation =
    StationsInformation(
        stations = stations.map { station ->
            station.toNeTEx(operator)
        }
    )

fun StationInformation.toNeTEx(operator: Operator): StationInformation =
    StationInformation(
        station_id = mapIdToNeTEx(station_id, operator),
        name = name,
        address = address,
        lat = lat,
        lon = lon,
        capacity = capacity
    )

fun StationStatuses.toNeTEx(operator: Operator): StationStatuses =
    StationStatuses(
        stations = stations.map { stationStatus -> stationStatus.toNeTEx(operator) }
    )

fun StationStatus.toNeTEx(operator: Operator): StationStatus =
    StationStatus(
        station_id = mapIdToNeTEx(station_id, operator),
        is_installed = is_installed,
        is_renting = is_renting,
        is_returning = is_returning,
        last_reported = last_reported,
        num_bikes_available = num_bikes_available,
        num_docks_available = num_docks_available
    )

fun getDiscovery(gbfsStandard: Map<GbfsStandardEnum, String>): GBFSResponse =
    GBFSResponse.DiscoveryResponse(
        last_updated = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
        ttl = 15,
        data = Discovery(
            nb = DiscoveryLanguage(
                feeds = listOf(
                    DiscoveryFeed(
                        name = "system_information",
                        url = gbfsStandard.getValue(GbfsStandardEnum.system_information)
                    ),
                    DiscoveryFeed(
                        name = "station_information",
                        url = gbfsStandard.getValue(GbfsStandardEnum.station_information)
                    ),
                    DiscoveryFeed(
                        name = "station_status",
                        url = gbfsStandard.getValue(GbfsStandardEnum.station_status)
                    ),
                    DiscoveryFeed(
                        name = "system_pricing_plans",
                        url = gbfsStandard.getValue(GbfsStandardEnum.system_pricing_plans)
                    )
                )
            )
        )
    )

fun getGbfsEndpoint(operator: Operator, host: String, port: Int): Map<GbfsStandardEnum, String> {
    val modifiedHost = host.replace("bikeservice", "api")
    val urlHost = if (modifiedHost == "localhost") "http://$modifiedHost:$port" else "https://$modifiedHost/mobility/v1/bikes"
    return mapOf(
        GbfsStandardEnum.gbfs to "$urlHost/$operator/gbfs.json".toLowerCase(),
        GbfsStandardEnum.system_information to "$urlHost/$operator/system_information.json".toLowerCase(),
        GbfsStandardEnum.station_information to "$urlHost/$operator/station_information.json".toLowerCase(),
        GbfsStandardEnum.station_status to "$urlHost/$operator/station_status.json".toLowerCase(),
        GbfsStandardEnum.system_pricing_plans to "$urlHost/$operator/system_pricing_plans.json".toLowerCase()
    )
}
