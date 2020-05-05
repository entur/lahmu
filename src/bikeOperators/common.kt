package bikeOperators

import GbfsStandard
import getGbfsEndpoint
import org.entur.bikeOperators.bergenBysykkelURL
import org.entur.bikeOperators.kolumbusBysykkelURL
import org.entur.bikeOperators.osloBysykkelURL
import org.entur.bikeOperators.trondheimBysykkelURL

enum class Operators {
    OSLOBYSYKKEL, BERGENBYSYKKEL, TRONDHEIMBYSYKKEL, KOLUMBUSBYSYKKEL;

    companion object {
        fun isUrbanSharing(operators: Operators) = operators !== KOLUMBUSBYSYKKEL
    }
}

fun getOperators(port: String, host: Int): Map<String, List<Map<String, GbfsStandard>>> =
    mapOf("operators" to Operators.values().map {
        mapOf("$it".toLowerCase() to getGbfsEndpoint(it, port, host))
    })

fun getOperator(operator: Operators): GbfsStandard =
    when (operator) {
        Operators.OSLOBYSYKKEL -> osloBysykkelURL
        Operators.BERGENBYSYKKEL -> bergenBysykkelURL
        Operators.TRONDHEIMBYSYKKEL -> trondheimBysykkelURL
        Operators.KOLUMBUSBYSYKKEL -> kolumbusBysykkelURL
    }
