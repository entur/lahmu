package org.entur.bikeOperators
import GbfsStandard
import org.entur.formatUrl

enum class UrbanSharingOperator {
    OSLOBYSYKKEL, BERGENBYSYKKEL, TRONDHEIMBYSYKKEL
}

fun getUrbanSharingOperators(host: String, port: Int): Map<String, List<Map<String, String>>> =
    mapOf("operators" to UrbanSharingOperator.values().map { mapOf("$it".toLowerCase() to formatUrl(getUrbanSharingOperator(
        it
    ).gbfs, host, port)) })

fun getUrbanSharingOperator(operator: UrbanSharingOperator): GbfsStandard =
    when (operator) {
        UrbanSharingOperator.OSLOBYSYKKEL -> OsloBysykkelURL
        UrbanSharingOperator.BERGENBYSYKKEL -> BergenBysykkelURL
        UrbanSharingOperator.TRONDHEIMBYSYKKEL -> TrondheimBysykkelURL
    }

object OsloBysykkelURL : GbfsStandard {
    override val gbfs: String = "https://gbfs.urbansharing.com/oslobysykkel.no/gbfs.json"
    override val system_information: String = "https://gbfs.urbansharing.com/oslobysykkel.no/system_information.json"
    override val station_information: String = "https://gbfs.urbansharing.com/oslobysykkel.no/station_information.json"
    override val station_status: String = "https://gbfs.urbansharing.com/oslobysykkel.no/station_status.json"
}

object BergenBysykkelURL : GbfsStandard {
    override val gbfs: String = "https://gbfs.urbansharing.com/bergenbysykkel.no/gbfs.json"
    override val system_information: String = "https://gbfs.urbansharing.com/bergenbysykkel.no/system_information.json"
    override val station_information: String = "https://gbfs.urbansharing.com/bergenbysykkel.no/station_information.json"
    override val station_status: String = "https://gbfs.urbansharing.com/bergenbysykkel.no/station_status.json"
}

object TrondheimBysykkelURL : GbfsStandard {
    override val gbfs: String = "https://gbfs.urbansharing.com/trondheimbysykkel.no/gbfs.json"
    override val system_information: String = "https://gbfs.urbansharing.com/trondheimbysykkel.no/system_information.json"
    override val station_information: String = "https://gbfs.urbansharing.com/trondheimbysykkel.no/station_information.json"
    override val station_status: String = "https://gbfs.urbansharing.com/trondheimbysykkel.no/station_status.json"
}
