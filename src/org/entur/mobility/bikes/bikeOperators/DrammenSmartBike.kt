package org.entur.mobility.bikes.bikeOperators

import java.time.LocalDateTime
import java.time.ZoneOffset
import org.entur.mobility.bikes.GBFSResponse
import org.entur.mobility.bikes.GbfsStandardEnum
import org.entur.mobility.bikes.StationInformation
import org.entur.mobility.bikes.StationStatus
import org.entur.mobility.bikes.StationStatuses
import org.entur.mobility.bikes.StationsInformation
import org.entur.mobility.bikes.SystemInformation
import org.entur.mobility.bikes.SystemPricePlan
import org.entur.mobility.bikes.TTL
import org.entur.mobility.bikes.epochOf31Dec2020
import org.entur.mobility.bikes.epochOf5thJune2020

fun drammenBysykkelURL(access_token: String) = mapOf(
    GbfsStandardEnum.gbfs to "",
    GbfsStandardEnum.system_information to "",
    GbfsStandardEnum.station_information to "https://drammen.pub.api.smartbike.com/api/en/v3/stations.json?access_token=$access_token",
    GbfsStandardEnum.station_status to "https://drammen.pub.api.smartbike.com/api/en/v3/stations/status.json?access_token=$access_token",
    GbfsStandardEnum.system_pricing_plans to ""
)

data class DrammenAccessToken(
    val access_token: String,
    val expires_in: Long,
    val token_type: String,
    val scope: String?
)

data class DrammenStationsStatusResponse(val stationsStatus: List<DrammenStationStatus>)
data class DrammenStationStatus(
    val id: String,
    val status: DrammenStatusEnum,
    val availability: DrammenAvailability
)

enum class DrammenStatusEnum {
    OPN, CLS
}

data class DrammenAvailability(
    val bikes: Int,
    val slots: Int
)

data class DrammenStationsResponse(val stations: List<DrammenStation>)
data class DrammenStation(
    val id: String,
    val name: String,
    val address: String,
    val addressNumber: String?,
    val zipCode: String?,
    val districtCode: String?,
    val districtName: String?,
    val altitude: String?,
    val location: DrammenLocation,
    val stationType: String
)

data class DrammenLocation(
    val lat: String,
    val lon: String
)

fun drammenSystemInformation() = GBFSResponse.SystemInformationResponse(
    last_updated = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
    ttl = TTL,
    data = SystemInformation(
        system_id = "drammen",
        language = "nb",
        name = "Drammen Bysykkel",
        timezone = "Europe/Oslo",
        operator = null,
        phone_number = null,
        email = null
    )
)

fun drammenSystemPricingPlan() = GBFSResponse.SystemPricingPlans(
    last_updated = epochOf5thJune2020,
    ttl = getSecondsFrom(epochOf5thJune2020, epochOf31Dec2020),
    plans = listOf(
        SystemPricePlan(
            plan_id = "8B00A621-82E8-4AC0-9B89-ABEAF99BD238",
            url = "https://www.drammenbysykler.no/nb/info/abonnementer-og-priser",
            name = PricePlan.SEASON_PASS.toString(),
            currency = "NOK",
            price = 130.0,
            is_taxable = 0,
            description = ""
        )
    )
)

fun DrammenStationsResponse.toStationInformation(statusResponse: GBFSResponse.StationStatusesResponse?) =
    GBFSResponse.StationsInformationResponse(
        last_updated = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
        ttl = TTL,
        data = StationsInformation(
            stations.map {
                StationInformation(
                    station_id = mapIdToNeTEx(it.id, Operator.DRAMMENBYSYKKEL),
                    address = it.address,
                    lat = it.location.lat.toBigDecimal(),
                    lon = it.location.lon.toBigDecimal(),
                    name = it.name,
                    capacity = statusResponse?.data?.stations?.find { status ->
                        status.station_id == mapIdToNeTEx(
                            it.id,
                            Operator.DRAMMENBYSYKKEL
                        )
                    }?.let { station -> station.num_bikes_available + station.num_docks_available } ?: 0
                )
            }
        )
    )

fun DrammenStationsStatusResponse.toStationStatuses() = GBFSResponse.StationStatusesResponse(
    last_updated = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
    ttl = TTL,
    data = StationStatuses(
        stationsStatus.map {
            StationStatus(
                station_id = mapIdToNeTEx(it.id, Operator.DRAMMENBYSYKKEL),
                is_installed = 1,
                is_renting = if (it.status == DrammenStatusEnum.OPN) 1 else 0,
                is_returning = if (it.status == DrammenStatusEnum.OPN) 1 else 0,
                last_reported = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC).toBigDecimal(),
                num_bikes_available = it.availability.bikes,
                num_docks_available = it.availability.slots
            )
        }
    )
)
