package org.entur.mobility.bikes

const val POLL_INTERVAL_MS = 10 * 60000L
const val TIME_TO_LIVE_CACHE_SEC = 60L
const val TTL = 15L

val LILLESTROM_API_KEY = System.getenv("LILLESTROM_API_KEY")

val DRAMMEN_PUBLIC_ID = System.getenv("DRAMMEN_PUBLIC_ID")
val DRAMMEN_SECRET = System.getenv("DRAMMEN_SECRET")
var DRAMMEN_ACCESS_TOKEN: String = ""

val DRAMMEN_ACCESS_TOKEN_URL = "https://drammen.pub.api.smartbike.com/oauth/v2/token?client_id=$DRAMMEN_PUBLIC_ID&client_secret=$DRAMMEN_SECRET&grant_type=client_credentials"
