package org.entur.bikeOperators
import BikeResponse
import BikeResponseData
import BikeResponseFeed
import BikeResponseLanguage
import GBFSResponse
import Station
import StationInformationResponse
import StationStatus
import StationStatusResponse
import StationStatuses
import Stations
import SystemInformation
import SystemInformationResponse
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset

data class KolumbusStation(
    val id: String,
    val external_id: String,
    val name: String,
    val description: String?,
    val capacity: Int,
    val available_slots: Int,
    val available_vehicles: Int,
    val reserved_vehicles: Int,
    val reserved_slots: Int,
    val type: String,
    val latitude: BigDecimal,
    val longitude: BigDecimal
)
data class KolumbusResponse(val data: List<KolumbusStation>)
fun KolumbusResponse.toSystemInformation() = SystemInformationResponse(
    last_updated = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
    ttl = 15,
    data = SystemInformation(
        system_id = "kolumbusbysykkel",
        language = "nb",
        name = "Kolumbus bysykkel",
        timezone = "Europe/Oslo",
        operator = null,
        phone_number = null,
        email = null
    )
)
fun KolumbusResponse.toStationStatus() = StationStatusResponse(
    last_updated = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
    ttl = 15,
    data = StationStatuses(stations = data.map {
        StationStatus(
                station_id = it.id,
                        is_installed = 1,
                        is_renting = 1,
                        is_returning = 1,
                        last_reported = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC).toBigDecimal(),
                        num_bikes_available = it.available_vehicles,
                        num_docks_available = it.available_slots
    ) }
    )
)
fun KolumbusResponse.toStationInformation() = StationInformationResponse(
    last_updated = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
    ttl = 15,
    data = Stations(stations = data.map {
        Station(
            station_id = it.id,
            name = it.name,
            address = null,
            lat = it.latitude,
            lon = it.longitude,
            capacity = it.capacity
        ) }
    )
)
fun KolumbusResponse.toBikeResponse(): GBFSResponse<BikeResponseData> = BikeResponse(
    last_updated = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
    ttl = 15,
    data = BikeResponseData(nb =
    BikeResponseLanguage(feeds = listOf(
            BikeResponseFeed(name = "system_information", url = "kolumbusbysykkel/system_information.json"),
            BikeResponseFeed(name = "station_information", url = "kolumbusbysykkel/station_information.json"),
            BikeResponseFeed(name = "station_status", url = "kolumbusbysykkel/station_status.json")
)
    )
    )
)

val KolumbusStationInformation = "https://sanntidapi-web-prod.azurewebsites.net/api/parkings?type=CityBike"
