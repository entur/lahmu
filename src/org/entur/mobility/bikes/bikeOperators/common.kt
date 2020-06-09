package org.entur.mobility.bikes.bikeOperators

import org.entur.mobility.bikes.GbfsStandardEnum
import org.entur.mobility.bikes.bikeOperators.Operator.Companion.getCodeSpace
import org.entur.mobility.bikes.getGbfsEndpoint

enum class Operator {
    OSLOBYSYKKEL, BERGENBYSYKKEL, TRONDHEIMBYSYKKEL, KOLUMBUSBYSYKKEL, LILLESTROMBYSYKKEL, DRAMMENBYSYKKEL;

    companion object {
        fun Operator.isUrbanSharing() = this == OSLOBYSYKKEL || this == BERGENBYSYKKEL || this == TRONDHEIMBYSYKKEL
        fun Operator.isKolumbus() = this == KOLUMBUSBYSYKKEL
        fun Operator.isJCDecaux() = this == LILLESTROMBYSYKKEL
        fun Operator.isDrammenSmartBike() = this == DRAMMENBYSYKKEL
        fun Operator.getCodeSpace() = when (this) {
            OSLOBYSYKKEL -> "YOS"
            BERGENBYSYKKEL -> "YBE"
            TRONDHEIMBYSYKKEL -> "YTR"
            KOLUMBUSBYSYKKEL -> "YKO"
            LILLESTROMBYSYKKEL -> "YLI"
            DRAMMENBYSYKKEL -> "YDR"
        }

        fun Operator.getFetchUrls(accessToken: String = "") = when (this) {
            OSLOBYSYKKEL -> osloBysykkelURL
            BERGENBYSYKKEL -> bergenBysykkelURL
            TRONDHEIMBYSYKKEL -> trondheimBysykkelURL
            KOLUMBUSBYSYKKEL -> kolumbusBysykkelURL
            LILLESTROMBYSYKKEL -> lillestromBysykkelURL
            DRAMMENBYSYKKEL -> drammenBysykkelURL(accessToken)
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

fun getSecondsFrom(fromEpoch: Long, toEpoch: Long): Long {
    val diffMS = toEpoch - fromEpoch
    return if (diffMS > 0) diffMS / 1000 else 0
}

enum class PricePlan {
    SEASON_PASS, DAY_PASS_30, DAY_PASS_3, DAY_PASS_1, ADD_ON_PASS;

    override fun toString() = when (this) {
        SEASON_PASS -> "sesongkort"
        DAY_PASS_30 -> "30-dagerskort"
        DAY_PASS_1 -> "dagskort"
        ADD_ON_PASS -> "tilleggskort"
        DAY_PASS_3 -> "3-dagerskort"
    }
}
