package org.entur.mobility.bikes

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.entur.mobility.bikes.bikeOperators.DrammenAccessToken
import org.entur.mobility.bikes.bikeOperators.JCDecauxStation
import org.entur.mobility.bikes.bikeOperators.KolumbusStation
import org.entur.mobility.bikes.bikeOperators.OttoLocation
import org.entur.mobility.bikes.bikeOperators.OttoLocationIdResponse
import org.entur.mobility.bikes.bikeOperators.OttoLocationsResponse
import org.entur.mobility.bikes.bikeOperators.kolumbusBysykkelURL
import org.entur.mobility.bikes.bikeOperators.lillestromBysykkelURL
import org.entur.mobility.bikes.bikeOperators.ottoBysykkelUrl

val client = HttpClient()

inline fun <reified T> parseResponse(url: String, bearer: String? = null): T {
    val response = fetch(url, bearer)
    return Gson().fromJson(response, T::class.java)
}

fun parseKolumbusResponse(): List<KolumbusStation> {
    val response = fetch(
        kolumbusBysykkelURL.getValue(GbfsStandardEnum.system_information)
    )
    val itemType = object : TypeToken<List<KolumbusStation>>() {}.type
    return Gson().fromJson(response, itemType)
}

fun parseJCDecauxResponse(): List<JCDecauxStation> {
    val response = fetch(
        lillestromBysykkelURL.getValue(GbfsStandardEnum.system_information)
    )
    val itemType = object : TypeToken<List<JCDecauxStation>>() {}.type
    return Gson().fromJson(response, itemType)
}

fun parseOttoResponse(): List<OttoLocation> {
    val response = fetch(
        ottoBysykkelUrl()
            .getValue(GbfsStandardEnum.station_information),
        OTTO_BEARER_TOKEN
    )
    val itemType = object : TypeToken<List<OttoLocation>>() {}.type
    return Gson().fromJson(response, itemType)
}

fun fetchAndSetDrammenAccessToken() {
    val response = try {
        logger.info("Fetching Drammen access token.")
        parseResponse<DrammenAccessToken>(
            DRAMMEN_ACCESS_TOKEN_URL
        )
    } catch (e: Exception) {
        logger.error("Failed to fetch Drammen access token. $e")
        null
    }
    DRAMMEN_ACCESS_TOKEN = response?.access_token ?: ""
}

fun fetch(url: String, bearer: String? = null): String {
    val response = runBlocking {
        client.get<String>(url) {
            header("Client-Identifier", "entur-bikeservice")
            header("Authorization", if (bearer != null) "Bearer $bearer" else null)
        }
    }
    return response
}

fun fetchListOfIdsFlow(
    ottoLocationsResponse: OttoLocationsResponse
): Flow<List<OttoLocationIdResponse?>> =
        flow {
            val listResults = ottoLocationsResponse.data.map {
                parseResponse<OttoLocationIdResponse>(
                    ottoBysykkelUrl(it.location_id.toString()).getValue(
                        GbfsStandardEnum.station_status
                    ),
                    OTTO_BEARER_TOKEN
                )
            }
            emit(listResults)
        }
