package com.boardgamegeek.auth

import android.text.TextUtils
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.text.SimpleDateFormat
import java.util.*

class BggCookieJar : CookieJar {
    var authToken = ""
        private set
    var authTokenExpiry = 0L
        private set

    fun isValid() = !TextUtils.isEmpty(authToken)

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val authCookie = cookies.find { "bggpassword".equals(it.name(), ignoreCase = true) }
        authToken = authCookie?.value() ?: ""
        authTokenExpiry = authCookie?.expiresAt() ?: 0
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> = emptyList()

    override fun toString(): String {
        val dt = SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault())
        return "token: $authToken (expires ${dt.format(Date(authTokenExpiry))})"
    }

    companion object {
        val mock: BggCookieJar
            get() {
                val authResponse = BggCookieJar()
                authResponse.authToken = "password"
                authResponse.authTokenExpiry = Long.MAX_VALUE
                return authResponse
            }
    }
}
