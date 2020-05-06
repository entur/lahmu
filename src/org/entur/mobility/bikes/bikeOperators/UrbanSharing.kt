package org.entur.mobility.bikes.bikeOperators

import org.entur.mobility.bikes.GbfsStandard

val osloBysykkelURL = GbfsStandard(
    gbfs = "https://gbfs.urbansharing.com/oslobysykkel.no/gbfs.json",
    system_information = "https://gbfs.urbansharing.com/oslobysykkel.no/system_information.json",
    station_information = "https://gbfs.urbansharing.com/oslobysykkel.no/station_information.json",
    station_status = "https://gbfs.urbansharing.com/oslobysykkel.no/station_status.json"
)

val bergenBysykkelURL = GbfsStandard(
    gbfs = "https://gbfs.urbansharing.com/bergenbysykkel.no/gbfs.json",
    system_information = "https://gbfs.urbansharing.com/bergenbysykkel.no/system_information.json",
    station_information = "https://gbfs.urbansharing.com/bergenbysykkel.no/station_information.json",
    station_status = "https://gbfs.urbansharing.com/bergenbysykkel.no/station_status.json"
)

val trondheimBysykkelURL = GbfsStandard(
    gbfs = "https://gbfs.urbansharing.com/trondheimbysykkel.no/gbfs.json",
    system_information = "https://gbfs.urbansharing.com/trondheimbysykkel.no/system_information.json",
    station_information = "https://gbfs.urbansharing.com/trondheimbysykkel.no/station_information.json",
    station_status = "https://gbfs.urbansharing.com/trondheimbysykkel.no/station_status.json"
)
