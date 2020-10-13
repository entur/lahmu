package org.entur.mobility.bikes

import com.google.gson.Gson
import java.io.UnsupportedEncodingException
import java.lang.reflect.Type
import java.net.URLEncoder

@Throws(UnsupportedEncodingException::class)
fun sanitize(inputValue: String): String {
    // https://en.wikipedia.org/wiki/HTTP_response_splitting
    return if (!containsNumbersLowercaseLettersAndDashes(inputValue)) {
        URLEncoder.encode(inputValue, "UTF-8")
    } else inputValue
}

fun containsNumbersLowercaseLettersAndDashes(inputValue: String): Boolean {
    for (element in inputValue) {
        if (!Character.isDigit(element) && element != '-' && (element < 'a' || element > 'z')) {
            return false
        }
    }
    return true
}

inline fun <reified T> parseResponse(response: String): T {
    return parseResponse(response, T::class.java)
}

fun <T> parseResponse(response: String, type: Type): T {
    return Gson().fromJson(response, type)
}
