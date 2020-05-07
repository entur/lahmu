package org.entur.mobility.bikes

import java.time.LocalDateTime
import org.entur.mobility.bikes.bikeOperators.Operator

interface Cache<T> {
    val cacheMap: HashMap<Operator, T>
    val lastUpdated: LocalDateTime
}

class InMemoryCache<T>(
    override val cacheMap: HashMap<Operator, T>,
    override var lastUpdated: LocalDateTime

) : Cache<T> {
    fun getResponseFromCache(bikeOperator: Operator): T? =
        cacheMap[bikeOperator]

    fun setResponseInCacheAndGet(bikeOperator: Operator, response: T): T {
        cacheMap[bikeOperator] = response
        lastUpdated = LocalDateTime.now()
        return response
    }
    fun isValidCache(bikeOperator: Operator): Boolean =
        cacheMap[bikeOperator] != null && lastUpdated > LocalDateTime.now().minusSeconds(15)
}
