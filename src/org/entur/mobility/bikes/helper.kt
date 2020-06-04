package org.entur.mobility.bikes

import java.io.UnsupportedEncodingException
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

fun booleanToInt(bool: Boolean) = if (bool) 1 else 0
