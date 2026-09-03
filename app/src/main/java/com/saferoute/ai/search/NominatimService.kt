package com.saferoute.ai.search

import com.saferoute.ai.navigation.httpGetText
import com.saferoute.ai.model.SearchResult
import com.saferoute.ai.model.*
import org.json.JSONArray
import org.osmdroid.util.GeoPoint
import java.net.URLEncoder
import com.saferoute.ai.navigation.httpGetText

// ============================================================

fun searchNominatim(
    query: String,
    currentLocation: GeoPoint?
): List<SearchResult> {

    val encoded =
        URLEncoder.encode(
            query.trim(),
            "UTF-8"
        )

    var urlString =
        "https://nominatim.openstreetmap.org/search" +
                "?q=$encoded" +
                "&format=jsonv2" +
                "&limit=8" +
                "&countrycodes=in" +
                "&addressdetails=1"

    currentLocation?.let {

        val delta = 0.5

        val left =
            it.longitude - delta

        val right =
            it.longitude + delta

        val top =
            it.latitude + delta

        val bottom =
            it.latitude - delta

        urlString +=
            "&viewbox=$left,$top,$right,$bottom"
    }

    return httpGetText(
        urlString = urlString,
        timeoutMillis = 15000
    ).let { response ->

        val array =
            JSONArray(response)

        val results =
            mutableListOf<SearchResult>()

        for (
            i in 0 until array.length()
        ) {

            val item =
                array.getJSONObject(i)

            val name =
                item.optString(
                    "display_name"
                )

            val latitude =
                item.optString(
                    "lat"
                ).toDoubleOrNull()

            val longitude =
                item.optString(
                    "lon"
                ).toDoubleOrNull()

            if (
                name.isNotBlank() &&
                latitude != null &&
                longitude != null
            ) {

                results.add(
                    SearchResult(
                        displayName = name,
                        latitude = latitude,
                        longitude = longitude
                    )
                )
            }
        }

        results
    }
}

// ============================================================
