package bikeOperators

import GbfsStandard
import org.entur.bikeOperators.BergenBysykkelURL
import org.entur.bikeOperators.KolumbusBysykkelURL
import org.entur.bikeOperators.OsloBysykkelURL
import org.entur.bikeOperators.TrondheimBysykkelURL
import org.entur.formatUrl

enum class Operators {
    OSLOBYSYKKEL, BERGENBYSYKKEL, TRONDHEIMBYSYKKEL, KOLUMBUSBYSYKKEL;

    companion object {
        fun isUrbanSharing(operators: Operators) = operators !== KOLUMBUSBYSYKKEL
    }
}

fun getOperators(port: String, host: Int): Map<String, List<Map<String, String>>> =
    mapOf("operators" to Operators.values().map { mapOf("$it".toLowerCase() to formatUrl(getOperator(
        it
    ).gbfs, port, host)) })

fun getOperator(operator: Operators): GbfsStandard =
    when (operator) {
        Operators.OSLOBYSYKKEL -> OsloBysykkelURL
        Operators.BERGENBYSYKKEL -> BergenBysykkelURL
        Operators.TRONDHEIMBYSYKKEL -> TrondheimBysykkelURL
        Operators.KOLUMBUSBYSYKKEL -> KolumbusBysykkelURL
    }
