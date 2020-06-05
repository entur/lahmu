package org.entur.mobility.bikes.bikeOperators

import java.math.BigDecimal
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

fun KolumbusResponse.jcDecauxSystemInformation(): GBFSResponse.SystemInformationResponse =
    GBFSResponse.SystemInformationResponse(
        last_updated = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
        ttl = TTL,
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

fun KolumbusResponse.toStationStatus(): GBFSResponse.StationStatusesResponse =
    GBFSResponse.StationStatusesResponse(
        last_updated = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
        ttl = TTL,
        data = StationStatuses(stations = data.map {
            StationStatus(
                station_id = mapIdToNeTEx(it.external_id, Operator.KOLUMBUSBYSYKKEL),
                is_installed = 1,
                is_renting = 1,
                is_returning = 1,
                last_reported = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC).toBigDecimal(),
                num_bikes_available = it.available_vehicles,
                num_docks_available = it.available_slots
            )
        })
    )

fun KolumbusResponse.toStationInformation(): GBFSResponse.StationsInformationResponse =
    GBFSResponse.StationsInformationResponse(
        last_updated = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
        ttl = TTL,
        data = StationsInformation(stations = data.map {
            StationInformation(
                station_id = mapIdToNeTEx(it.external_id, Operator.KOLUMBUSBYSYKKEL),
                name = it.name,
                address = null,
                lat = it.latitude,
                lon = it.longitude,
                capacity = it.capacity
            )
        })
    )

fun kolumbusSystemPricingPlans() = GBFSResponse.SystemPricingPlans(
    last_updated = epochOf5thJune2020,
    ttl = getSecondsFrom(epochOf5thJune2020, epochOf31Dec2020),
    plans = listOf(
        SystemPricePlan(
            plan_id = "636B0671-ED87-42FB-8FAC-6AE8F3A25826",
            url = "https://www.kolumbus.no/Billetter/-priser-og-produkter/bysykkelbillett/",
            name = PricePlan.DAY_PASS_30.toString(),
            currency = "NOK",
            price = 125.0,
            is_taxable = 0,
            description = "For usage above an hour, it will follow a running cost of 1.0 NOK per minute."
        ),
        SystemPricePlan(
            plan_id = "2AFBF7AD-4EE6-483F-A32A-3A8C94840996",
            url = "https://www.kolumbus.no/verdt-a-vite/sykkel-oversikt/bysykkelen/",
            name = PricePlan.ADD_ON_PASS.toString(),
            currency = "NOK",
            price = 0.0,
            is_taxable = 0,
            description = "With any valid ticket in the Kolumbus app, the city bike is available for free up to an hour. " +
                "For usage above an hour, it will follow a running cost of 1.0 NOK per minute."
        )
    )
)

val kolumbusBysykkelURL = mapOf(
    GbfsStandardEnum.gbfs to "",
    GbfsStandardEnum.system_information to "https://sanntidapi-web-prod.azurewebsites.net/api/parkings?type=CityBike",
    GbfsStandardEnum.station_information to "https://sanntidapi-web-prod.azurewebsites.net/api/parkings?type=CityBike",
    GbfsStandardEnum.station_status to "https://sanntidapi-web-prod.azurewebsites.net/api/parkings?type=CityBike",
    GbfsStandardEnum.system_pricing_plans to ""
)
