package org.entur.mobility.bikes

import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneOffset

val env: String = System.getenv("ENVIRONMENT_NAMESPACE") ?: "local"
val isProd = env == "production"

// The poll interval should probably be somewhere between 10sec and 60sec. As of 04.06.2020, Urban Sharing delivers data
// with the lowest TTL of the operators (10 sec). Therefore, there are no reason to poll more often than that.
// However, to protect the other operators, we recommend to increase the interval a bit.
val POLL_INTERVAL_MS = if (isProd) 60000L else (10 * 60000L)
// The TTL should be of same value as the Poll Interval, but in order to avoid some corner cases, we increase it with a second
val TTL = (POLL_INTERVAL_MS / 1000) + 1
const val TIME_TO_LIVE_DRAMMEN_ACCESS_KEY_MS = 60 * 60000L

val LILLESTROM_API_KEY = System.getenv("LILLESTROM_API_KEY")

val DRAMMEN_PUBLIC_ID = System.getenv("DRAMMEN_PUBLIC_ID")
val DRAMMEN_SECRET = System.getenv("DRAMMEN_SECRET")
var DRAMMEN_ACCESS_TOKEN: String = ""

val DRAMMEN_ACCESS_TOKEN_URL = "https://drammen.pub.api.smartbike.com/oauth/v2/token?client_id=$DRAMMEN_PUBLIC_ID&client_secret=$DRAMMEN_SECRET&grant_type=client_credentials"

val epochOf5thJune2020 = LocalDateTime.of(2020, Month.JUNE, 5, 0, 0).toEpochSecond(ZoneOffset.UTC)
val epochOf31Dec2020 = LocalDateTime.of(2020, Month.DECEMBER, 31, 23, 59).toEpochSecond(ZoneOffset.UTC)
