package com.boardgamegeek.auth

import com.boardgamegeek.util.HttpUtils
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.LoginEvent
import hugo.weaving.DebugLog
import okhttp3.FormBody
import okhttp3.Request
import timber.log.Timber
import java.io.IOException

object NetworkAuthenticator {
    private val MOCK_LOGIN = false

    /**
     * Authenticates to BGG with the specified username and password, returning the cookie store to use on subsequent
     * requests, or null if authentication fails.
     */
    fun authenticate(username: String, password: String, method: String): BggCookieJar? {
        return when {
            MOCK_LOGIN -> BggCookieJar.mock
            else -> tryAuthenticate(username, password, method)
        }
    }

    private fun tryAuthenticate(username: String, password: String, method: String): BggCookieJar? {
        try {
            return performAuthenticate(username, password, method)
        } catch (e: IOException) {
            logAuthFailure(method, "IOException")
        } finally {
            Timber.w("Authentication complete")
        }
        return null
    }

    @DebugLog
    @Throws(IOException::class)
    private fun performAuthenticate(username: String, password: String, method: String): BggCookieJar? {
        val cookieJar = BggCookieJar()
        val client = HttpUtils.getHttpClient().newBuilder()
                .cookieJar(cookieJar)
                .build()
        val post = buildRequest(username, password)
        val response = client.newCall(post).execute()
        if (response.isSuccessful) {
            if (cookieJar.isValid()) {
                Answers.getInstance().logLogin(LoginEvent()
                        .putMethod(method)
                        .putSuccess(true))
                return cookieJar
            } else {
                logAuthFailure(method, "Invalid cookie jar")
            }
        } else {
            logAuthFailure(method, "Response: ${response.message()}")
        }
        return null
    }

    private fun logAuthFailure(method: String, reason: String) {
        Timber.w("Failed $method login: $reason")
        Answers.getInstance().logLogin(LoginEvent()
                .putMethod(method)
                .putSuccess(false)
                .putCustomAttribute("Reason", reason))
    }

    @DebugLog
    private fun buildRequest(username: String, password: String): Request {
        val formBody = FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .build()
        return Request.Builder()
                .url("https://www.boardgamegeek.com/login")
                .post(formBody)
                .build()
    }
}
