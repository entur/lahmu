package org.entur.lahmu.domain

import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProviderDiscovery(
    @Required @SerialName("last_updated") val lastUpdated: Long,
    @Required val ttl: Int,
    @Required val data: ProviderDiscoveryData
) {

    @Serializable
    data class ProviderDiscoveryData(
        @Required val providers: List<Provider>
    )

    @Serializable
    data class Provider(
        @Required val url: String
    )
}
