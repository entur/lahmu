package org.entur.mobility.bikes.bikeOperators

import org.entur.mobility.bikes.GbfsStandardEnum
import org.entur.mobility.bikes.bikeOperators.Operator.Companion.getCodeSpace
import org.entur.mobility.bikes.getGbfsEndpoint

enum class Operator {
    OSLOBYSYKKEL, BERGENBYSYKKEL, TRONDHEIMBYSYKKEL, KOLUMBUSBYSYKKEL, LILLESTROMBYSYKKEL, DRAMMENBYSYKKEL, OTTO;

    companion object {
        fun Operator.isUrbanSharing() = this == OSLOBYSYKKEL || this == BERGENBYSYKKEL || this == TRONDHEIMBYSYKKEL
        fun Operator.isKolumbus() = this == KOLUMBUSBYSYKKEL
        fun Operator.isJCDecaux() = this == LILLESTROMBYSYKKEL
        fun Operator.isDrammenSmartBike() = this == DRAMMENBYSYKKEL
        fun Operator.isOtto() = this == OTTO
        fun Operator.getCodeSpace() = when (this) {
            OSLOBYSYKKEL -> "YOS"
            BERGENBYSYKKEL -> "YBE"
            TRONDHEIMBYSYKKEL -> "YTR"
            KOLUMBUSBYSYKKEL -> "YKO"
            LILLESTROMBYSYKKEL -> "YLI"
            DRAMMENBYSYKKEL -> "YDR"
            OTTO -> "YOT"
        }

        fun Operator.getFetchUrls(id: String = "") = when (this) {
            OSLOBYSYKKEL -> osloBysykkelURL
            BERGENBYSYKKEL -> bergenBysykkelURL
            TRONDHEIMBYSYKKEL -> trondheimBysykkelURL
            KOLUMBUSBYSYKKEL -> kolumbusBysykkelURL
            LILLESTROMBYSYKKEL -> lillestromBysykkelURL
            DRAMMENBYSYKKEL -> drammenBysykkelURL(id)
            OTTO -> ottoBysykkelUrl(id)
        }
    }
}

fun getOperatorsWithDiscovery(port: String, host: Int): Map<String, List<Map<String, String>>> =
    mapOf("operators" to Operator.values().map {
        mapOf(
            "name" to "$it".toLowerCase(),
            "url" to getGbfsEndpoint(it, port, host).getValue(GbfsStandardEnum.gbfs)
        )
    })

fun mapIdToNeTEx(id: String, operator: Operator) = "${operator.getCodeSpace()}:VehicleSharingParkingArea:$id"
