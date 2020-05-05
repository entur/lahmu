package org.entur.bikeOperators
import GbfsStandard

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
