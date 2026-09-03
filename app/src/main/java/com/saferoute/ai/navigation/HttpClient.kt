package com.saferoute.ai.navigation

import java.net.HttpURLConnection
import java.net.URL

// ============================================================

fun httpGetText(
    urlString: String,
    timeoutMillis: Int
): String {

    val connection =
        URL(urlString)
            .openConnection()
                as HttpURLConnection

    connection.requestMethod = "GET"
    connection.connectTimeout =
        timeoutMillis
    connection.readTimeout =
        timeoutMillis
    connection.instanceFollowRedirects =
        true

    connection.setRequestProperty(
        "User-Agent",
        "SafeRouteAI/1.0 Android navigation app"
    )

    connection.setRequestProperty(
        "Accept",
        "application/json"
    )

    return try {

        val responseCode =
            connection.responseCode

        val stream =
            if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
                    ?: throw Exception(
                        "HTTP $responseCode"
                    )
            }

        val response =
            stream
                .bufferedReader()
                .use {
                    it.readText()
                }

        if (responseCode !in 200..299) {

            var serverMessage =
                response.trim()

            if (
                serverMessage.length > 300
            ) {
                serverMessage =
                    serverMessage.take(300)
            }

            throw Exception(
                "HTTP $responseCode: $serverMessage"
            )
        }

        response

    } finally {
        connection.disconnect()
    }
}

// ============================================================
