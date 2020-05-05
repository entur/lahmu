package org.entur

import bikeOperators.Operators
import java.time.LocalDateTime

interface Cache<T> {
    val cacheMap: HashMap<Operators, T>
    val lastUpdated: LocalDateTime
}

class InMemoryCache<T>(
    override val cacheMap: HashMap<Operators, T>,
    override var lastUpdated: LocalDateTime
) : Cache<T> {
    fun getResponseFromCache(bikeOperator: Operators) =
        cacheMap[bikeOperator]

    fun setResponseInCacheAndGet(bikeOperator: Operators, response: T): T {
        cacheMap[bikeOperator] = response
        lastUpdated = LocalDateTime.now()
        return response
    }
    fun isValidCache(bikeOperator: Operators): Boolean =
        cacheMap[bikeOperator] != null && lastUpdated > LocalDateTime.now().minusSeconds(5)
}
