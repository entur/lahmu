package org.entur.mobility.bikes

import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.entur.mobility.bikes.bikeOperators.Operators

data class GbfsStandard(
    val gbfs: String,
    val system_information: String,
    val station_information: String,
    val station_status: String
)

data class GBFSResponse<T> (
    val last_updated: Long,
    val ttl: Long,
    val data: T
)

data class Discovery(val nb: DiscoveryLanguage)
data class DiscoveryLanguage(val feeds: List<DiscoveryFeed>)
data class DiscoveryFeed(val name: String, val url: String)

data class SystemInformation(val system_id: String, val language: String, val name: String, val operator: String?, val timezone: String, val phone_number: String?, val email: String?)

data class Stations(val stations: List<Station>)
data class Station(val station_id: String, val name: String, val address: String?, val lat: BigDecimal, val lon: BigDecimal, val capacity: Int)

data class StationStatuses(val stations: List<StationStatus>)
data class StationStatus(val station_id: String, val is_installed: Int, val is_renting: Int, val is_returning: Int, val last_reported: BigDecimal, val num_bikes_available: Int, val num_docks_available: Int)

fun getDiscovery(gbfsStandard: GbfsStandard): GBFSResponse<Discovery> =
    GBFSResponse(
        last_updated = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
        ttl = 15,
        data = Discovery(
            nb = DiscoveryLanguage(
                feeds = listOf(
                    DiscoveryFeed(
                        name = "system_information",
                        url = gbfsStandard.system_information
                    ),
                    DiscoveryFeed(
                        name = "station_information",
                        url = gbfsStandard.station_information
                    ),
                    DiscoveryFeed(
                        name = "station_status",
                        url = gbfsStandard.station_status
                    )
                )
            )
        )
    )

fun getGbfsEndpoint(operators: Operators, host: String, port: Int): GbfsStandard {
    val modifiedHost = host.replace("bikeservice", "api")
    val urlHost = if (modifiedHost == "localhost") "http://$modifiedHost:$port" else "https://$modifiedHost/bikeservice"
    return GbfsStandard(
        gbfs = "$urlHost/$operators/gbfs.json".toLowerCase(),
        system_information = "$urlHost/$operators/system_information.json".toLowerCase(),
        station_information = "$urlHost/$operators/station_information.json".toLowerCase(),
        station_status = "$urlHost/$operators/station_status.json".toLowerCase()
    )
}
