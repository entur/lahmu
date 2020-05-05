import bikeOperators.Operators
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset

data class GbfsStandard(
    val gbfs: String,
    val system_information: String,
    val station_information: String,
    val station_status: String
)

interface GBFSResponse<T> {
    val last_updated: Long
    val ttl: Long
    val data: T
}

data class GbfsJsonResponse(override val last_updated: Long, override val ttl: Long, override val data: GbfsJsonData) :
    GBFSResponse<GbfsJsonData>
data class GbfsJsonData(val nb: GbfsJsonLanguage)
data class GbfsJsonLanguage(val feeds: List<GbfsJsonFeed>)
data class GbfsJsonFeed(val name: String, val url: String)

data class SystemInformationResponse(override val last_updated: Long, override val ttl: Long, override val data: SystemInformation) :
    GBFSResponse<SystemInformation>
data class SystemInformation(val system_id: String, val language: String, val name: String, val operator: String?, val timezone: String, val phone_number: String?, val email: String?)

data class StationInformationResponse(override val last_updated: Long, override val ttl: Long, override val data: Stations) :
    GBFSResponse<Stations>
data class Stations(val stations: List<Station>)
data class Station(val station_id: String, val name: String, val address: String?, val lat: BigDecimal, val lon: BigDecimal, val capacity: Int)

data class StationStatusResponse(override val last_updated: Long, override val ttl: Long, override val data: StationStatuses) :
    GBFSResponse<StationStatuses>
data class StationStatuses(val stations: List<StationStatus>)
data class StationStatus(val station_id: String, val is_installed: Int, val is_renting: Int, val is_returning: Int, val last_reported: BigDecimal, val num_bikes_available: Int, val num_docks_available: Int)

fun getGbfsJson(gbfsStandard: GbfsStandard): GbfsJsonResponse =
    GbfsJsonResponse(
        last_updated = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
        ttl = 15,
        data = GbfsJsonData(
            nb = GbfsJsonLanguage(
                feeds = listOf(
                    GbfsJsonFeed(name = "gbfs", url = gbfsStandard.gbfs),
                    GbfsJsonFeed(name = "system_information", url = gbfsStandard.system_information),
                    GbfsJsonFeed(name = "station_information", url = gbfsStandard.station_information),
                    GbfsJsonFeed(name = "station_status", url = gbfsStandard.station_status)
                )
            )
        )
    )

fun getGbfsEndpoint(operators: Operators, host: String, port: Int): GbfsStandard {
    val urlHost = if (host == "localhost") "$host:$port" else host
    return GbfsStandard(
        gbfs = "$urlHost/$operators/gbfs.json".toLowerCase(),
        system_information = "$urlHost/$operators/system_information.json".toLowerCase(),
        station_information = "$urlHost/$operators/station_information.json".toLowerCase(),
        station_status = "$urlHost/$operators/station_status.json".toLowerCase()
    )
}
