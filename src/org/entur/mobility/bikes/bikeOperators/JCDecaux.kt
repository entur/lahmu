package org.entur.mobility.bikes.bikeOperators

import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.entur.mobility.bikes.GBFSResponse
import org.entur.mobility.bikes.GbfsStandardEnum
import org.entur.mobility.bikes.LILLESTROM_API_KEY
import org.entur.mobility.bikes.StationInformation
import org.entur.mobility.bikes.StationStatus
import org.entur.mobility.bikes.StationStatuses
import org.entur.mobility.bikes.StationsInformation
import org.entur.mobility.bikes.SystemInformation
import org.entur.mobility.bikes.SystemPricePlan
import org.entur.mobility.bikes.TTL
import org.entur.mobility.bikes.epochOf31Dec2020
import org.entur.mobility.bikes.epochOf5thJune2020

val lillestromBysykkelURL = mapOf(
    GbfsStandardEnum.gbfs to "",
    GbfsStandardEnum.system_information to "https://api.jcdecaux.com/vls/v3/stations?contract=lillestrom&apiKey=$LILLESTROM_API_KEY",
    GbfsStandardEnum.station_information to "https://api.jcdecaux.com/vls/v3/stations?contract=lillestrom&apiKey=$LILLESTROM_API_KEY",
    GbfsStandardEnum.station_status to "https://api.jcdecaux.com/vls/v3/stations?contract=lillestrom&apiKey=$LILLESTROM_API_KEY",
    GbfsStandardEnum.system_pricing_plans to ""
)

data class JCDecauxResponse(val data: List<JCDecauxStation>)

data class JCDecauxStation(
    val number: Int,
    val contractName: String,
    val name: String,
    val address: String,
    val position: JCDecauxPosition,
    val banking: Boolean,
    val bonus: Boolean,
    val status: JCDecauxStatus,
    val lastUpdate: String,
    val connected: Boolean,
    val overflow: Boolean,
    val shape: Boolean,
    val totalStands: JCDecauxStands,
    val mainStands: JCDecauxStands,
    val overflowStands: JCDecauxStands?

)

data class JCDecauxPosition(
    val latitude: BigDecimal,
    val longitude: BigDecimal
)

enum class JCDecauxStatus {
    CLOSED, OPEN
}

data class JCDecauxStands(
    val availabilities: JCDecauxAvailabilities,
    val capacity: Int
)

data class JCDecauxAvailabilities(
    val bikes: Int,
    val stands: Int,
    val mechanicalBikes: Int,
    val electricalBikes: Int,
    val electricalInternalBatteryBikes: Int,
    val electricalRemovableBatteryBikes: Int
)

fun JCDecauxResponse.toStationInformation(): GBFSResponse.StationsInformationResponse =
    GBFSResponse.StationsInformationResponse(
        last_updated = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
        ttl = TTL,
        data = StationsInformation(
            stations = data.map {
                StationInformation(
                    station_id = mapIdToNeTEx(it.number.toString(), Operator.LILLESTROMBYSYKKEL),
                    name = it.name,
                    address = it.address,
                    lat = it.position.latitude,
                    lon = it.position.longitude,
                    capacity = it.number
                )
            }
        )
    )

fun jcDecauxSystemInformation(): GBFSResponse.SystemInformationResponse =
    GBFSResponse.SystemInformationResponse(
        last_updated = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
        ttl = TTL,
        data = SystemInformation(
            system_id = "lillestrom",
            language = "no",
            name = "Lillestrøm bysykkel",
            timezone = "Europe/Oslo",
            operator = null,
            phone_number = null,
            email = null
        )
    )

fun JCDecauxResponse.toStationStatus(): GBFSResponse.StationStatusesResponse =
    GBFSResponse.StationStatusesResponse(
        last_updated = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
        ttl = TTL,
        data = StationStatuses(stations = data.map {
            StationStatus(
                station_id = mapIdToNeTEx(it.number.toString(), Operator.LILLESTROMBYSYKKEL),
                is_installed = if (it.connected) 1 else 0,
                is_renting = if (it.status == JCDecauxStatus.OPEN) 1 else 0,
                is_returning = if (it.status == JCDecauxStatus.OPEN) 1 else 0,
                last_reported = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC).toBigDecimal(),
                num_bikes_available = it.totalStands.availabilities.bikes,
                num_docks_available = it.totalStands.availabilities.stands
            )
        })
    )

fun jcDecauxSystemPricingPlans(): GBFSResponse.SystemPricingPlans =
    GBFSResponse.SystemPricingPlans(
        last_updated = epochOf5thJune2020,
        ttl = getSecondsFrom(epochOf5thJune2020, epochOf31Dec2020),
        plans = listOf(
            SystemPricePlan(
                plan_id = "D16E7EC0-47F5-427D-9B71-CD079F989CC6",
                url = "http://www.bysykkel.org/Abonnement/Satser",
                name = PricePlan.SEASON_PASS.toString(),
                currency = "NOK",
                price = 50.0,
                is_taxable = 0,
                description = "Additional charges will run if the bike is unlocked continuously for more than 1 hour. " +
                    "The next half-hour will then cost 20.0 NOK, while every commenced half-hour after that costs 40.0 NOK"
            ),
            SystemPricePlan(
                plan_id = "867E4558-77E3-4608-8941-0C667E924280",
                url = "http://www.bysykkel.org/Abonnement/Satser",
                name = PricePlan.DAY_PASS_3.toString(),
                currency = "NOK",
                price = 10.0,
                is_taxable = 0,
                description = "Additional charges will run if the bike is unlocked continuously for more than 1 hour. " +
                    "The next half-hour will then cost 20.0 NOK, while every commenced half-hour after that costs 40.0 NOK"
            )
        )
    )
