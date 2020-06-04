package org.entur.mobility.bikes.bikeOperators

import java.math.BigInteger
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.entur.mobility.bikes.GBFSResponse
import org.entur.mobility.bikes.GbfsStandardEnum
import org.entur.mobility.bikes.StationInformation
import org.entur.mobility.bikes.StationStatus
import org.entur.mobility.bikes.StationStatuses
import org.entur.mobility.bikes.StationsInformation
import org.entur.mobility.bikes.SystemInformation
import org.entur.mobility.bikes.TTL
import org.entur.mobility.bikes.booleanToInt

fun ottoBysykkelUrl(id: String = "") = mapOf(
    GbfsStandardEnum.gbfs to "",
    GbfsStandardEnum.system_information to "",
    GbfsStandardEnum.station_information to "https://api.otto.no/locations",
    GbfsStandardEnum.station_status to "https://api.otto.no/locations/$id"
)

enum class OttoAvailability {
    available, no_support, noneAvailable;

    fun isInstalled() = this != no_support
    fun isRenting() = this == available
    fun isReturning() = this != no_support
}

data class OttoLocationsResponse(val data: List<OttoLocation>)
data class OttoLocation(
    val location_id: Int,
    val name: String,
    val address: String,
    val latitude: String,
    val longitude: String,
    val campaign: String?,
    val car: OttoAvailability,
    val bike: OttoAvailability,
    val scooter: OttoAvailability,
    val locked_garage: Boolean,
    val favourite: Boolean
)

fun OttoLocationsResponse.toStationInformation(ottoLocationIdResponse: List<OttoLocationIdResponse>) = GBFSResponse.StationsInformationResponse(
    last_updated = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
    ttl = TTL,
    data = StationsInformation(data.filter { it.bike !== OttoAvailability.no_support && !it.locked_garage }.map {
        val ottoLocation = ottoLocationIdResponse.find { location -> location.location_id == it.location_id }
        StationInformation(
            station_id = mapIdToNeTEx(it.location_id.toString(), Operator.OTTO),
            name = it.name,
            address = it.address,
            lat = it.latitude.toBigDecimal(),
            lon = it.longitude.toBigDecimal(),
            capacity = ottoLocation?.bikes?.size ?: 0
        )
    }
    )
)

fun OttoLocationsResponse.toSystemInformation() = GBFSResponse.SystemInformationResponse(
    last_updated = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
    ttl = TTL,
    data = SystemInformation(
        system_id = "otto",
        language = "no",
        name = "Otto fra Bertel O. Steen",
        operator = null,
        timezone = "Europe/Oslo",
        phone_number = null,
        email = "hei@otto.no"
    )
)

fun OttoLocationsResponse.toStationStatus(ottoLocationIdResponse: List<OttoLocationIdResponse>) =
    GBFSResponse.StationStatusesResponse(
        last_updated = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
        ttl = TTL,
        data = StationStatuses(data.filter { it.bike == OttoAvailability.available }.map {
            val ottoLocation = ottoLocationIdResponse.find { location -> location.location_id == it.location_id }
            StationStatus(
                station_id = mapIdToNeTEx(it.location_id.toString(), Operator.OTTO),
                is_installed = booleanToInt(it.bike.isInstalled()),
                is_renting = booleanToInt(it.bike.isRenting()),
                is_returning = booleanToInt(it.bike.isReturning()),
                last_reported = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC).toBigDecimal(),
                num_bikes_available = ottoLocation?.bikes?.size ?: 0,
                num_docks_available = 0
            )
        }
        )
    )

data class OttoMembership(
    val title: String,
    val description: String
)

data class OttoPrice(
    val hour: String,
    val day: String,
    val weekend: String,
    val week: String?,
    val km: String,
    val included_km: String?,
    val membership: OttoMembership?,
    val available_amount: Int,
    val image: String
)

data class OttoVehicle(
    val vehicle_id: Int,
    val licence_plate: String,
    val brand: String,
    val model: String,
    val is_electric: Boolean?,
    val auto_gearing: Boolean,
    val fuel_status: String,
    val estimated_range: String?,
    val description: String,
    val seats: Int,
    val available_from: BigInteger,
    val price: OttoPrice
)

data class OttoLocationIdResponse(
    val location_id: Int,
    val pre_title: String,
    val title: String,
    val name: String,
    val address: String,
    val campaign: Boolean,
    val cars: List<OttoVehicle>,
    val scooters: List<OttoVehicle>,
    val bikes: List<OttoVehicle>
)
