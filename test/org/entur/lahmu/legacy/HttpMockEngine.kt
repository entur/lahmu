package org.entur.lahmu.legacy

import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.headersOf
import org.entur.lahmu.legacy.bikeOperators.DrammenAccessToken

class HttpMockEngine {

    val client = HttpClient(MockEngine) {
        engine {
            addHandler { request ->
                when (request.url.toString()) {
                    "https://gbfs.urbansharing.com/oslobysykkel.no/system_information.json" ->
                        respondWithFixture("/oslobysykkelSystemInformation.json")
                    "https://gbfs.urbansharing.com/oslobysykkel.no/station_information.json" ->
                        respondWithFixture("/oslobysykkelStationInformation.json")
                    "https://gbfs.urbansharing.com/oslobysykkel.no/station_status.json" ->
                        respondWithFixture("/oslobysykkelStationStatus.json")
                    "https://sanntidapi-web-prod.azurewebsites.net/api/parkings?type=CityBike" ->
                        respondWithFixture("/kolumbusResponse.json")
                    "https://api.jcdecaux.com/vls/v3/stations?contract=lillestrom&apiKey=null" ->
                        respondWithFixture("/lillestrombysykkelStationsResponse.json")
                    "https://drammen.pub.api.smartbike.com/oauth/v2/token?client_id=null&client_secret=null&grant_type=client_credentials" ->
                        respondWithJson(DrammenAccessToken("test", 3600, "bearer", null))
                    "https://drammen.pub.api.smartbike.com/api/en/v3/stations/status.json?access_token=test" ->
                        respondWithFixture("/drammenStationsStatusResponse.json")
                    "https://drammen.pub.api.smartbike.com/api/en/v3/stations.json?access_token=test" ->
                        respondWithFixture("/drammenStationsResponse.json")
                    else -> error("Unhandled ${request.url}")
                }
            }
        }
    }

    private fun MockRequestHandleScope.respondWithFixture(fixturePath: String): HttpResponseData {
        return respond(
            getTestFixture(fixturePath), headers = responseHeaders
        )
    }

    private fun getTestFixture(resource: String): String {
        return this.javaClass.getResource(resource).readText()
    }

    private fun MockRequestHandleScope.respondWithJson(src: Any): HttpResponseData {
        return respond(Gson().toJson(src), headers = responseHeaders)
    }

    private val responseHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
}
