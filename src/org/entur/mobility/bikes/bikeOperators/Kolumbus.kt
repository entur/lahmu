package org.entur.mobility.bikes.bikeOperators

import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.entur.mobility.bikes.GBFSResponse
import org.entur.mobility.bikes.GbfsStandard
import org.entur.mobility.bikes.Station
import org.entur.mobility.bikes.StationStatus
import org.entur.mobility.bikes.StationStatuses
import org.entur.mobility.bikes.Stations
import org.entur.mobility.bikes.SystemInformation

data class KolumbusResponse(val data: List<KolumbusStation>)
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

fun KolumbusResponse.toSystemInformation(): GBFSResponse<SystemInformation> =
    GBFSResponse(
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

fun KolumbusResponse.toStationStatus(): GBFSResponse<StationStatuses> =
    GBFSResponse(
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
            )
        })
    )

fun KolumbusResponse.toStationInformation(): GBFSResponse<Stations> =
    GBFSResponse(
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
            )
        })
    )

val kolumbusBysykkelURL = GbfsStandard(
    gbfs = "",
    system_information = "https://sanntidapi-web-prod.azurewebsites.net/api/parkings?type=CityBike",
    station_information = "https://sanntidapi-web-prod.azurewebsites.net/api/parkings?type=CityBike",
    station_status = "https://sanntidapi-web-prod.azurewebsites.net/api/parkings?type=CityBike"
)
