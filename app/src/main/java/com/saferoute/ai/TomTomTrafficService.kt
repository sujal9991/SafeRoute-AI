package com.saferoute.ai.traffic

import android.util.Log
import com.saferoute.ai.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Live TomTom traffic incident access for SafeRoute AI.
 *
 * The API returns GeoJSON-style incident geometries.  Geometry can be a
 * Point, LineString, or a nested coordinate array, so the parser below
 * deliberately handles all of those forms instead of assuming that
 * coordinates is always [longitude, latitude].
 */
data class TomTomIncident(
    val id: String,
    val category: String,
    val magnitudeOfDelay: String,
    val description: String,
    val roadNumbers: List<String>,
    val delayInSeconds: Int?,
    val lengthInMeters: Double,
    val from: String?,
    val to: String?,
    val latitude: Double?,
    val longitude: Double?
)

object TomTomTrafficService {

    private const val TAG = "SafeRouteTomTom"

    private const val BASE_URL =
        "https://api.tomtom.com/maps/orbis/traffic/incidents/details"

    suspend fun getIncidents(
        minLongitude: Double,
        minLatitude: Double,
        maxLongitude: Double,
        maxLatitude: Double
    ): List<TomTomIncident> = withContext(Dispatchers.IO) {

        val apiKey = BuildConfig.TOMTOM_API_KEY.trim()

        require(apiKey.isNotEmpty()) {
            "TomTom API key is missing. Check local.properties."
        }

        val safeMinLongitude = minLongitude.coerceIn(-180.0, 180.0)
        val safeMaxLongitude = maxLongitude.coerceIn(-180.0, 180.0)
        val safeMinLatitude = minLatitude.coerceIn(-90.0, 90.0)
        val safeMaxLatitude = maxLatitude.coerceIn(-90.0, 90.0)

        require(safeMinLongitude < safeMaxLongitude) {
            "Invalid TomTom longitude bounding box."
        }
        require(safeMinLatitude < safeMaxLatitude) {
            "Invalid TomTom latitude bounding box."
        }

        val bbox = String.format(
            Locale.US,
            "%.6f,%.6f,%.6f,%.6f",
            safeMinLongitude,
            safeMinLatitude,
            safeMaxLongitude,
            safeMaxLatitude
        )

        val urlString =
            "$BASE_URL" +
                    "?apiVersion=2" +
                    "&bbox=$bbox" +
                    "&timeValidity=present"

        Log.d(TAG, "Requesting incidents: bbox=$bbox")

        val connection =
            URL(urlString).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.useCaches = false

            // TomTom recommends the API key in the header.
            connection.setRequestProperty(
                "TomTom-Api-Key",
                apiKey
            )

            connection.setRequestProperty(
                "TomTom-Api-Version",
                "2"
            )

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            connection.setRequestProperty(
                "Attributes",
                "incidents(" +
                        "type," +
                        "geometry(type,coordinates)," +
                        "properties(" +
                        "id," +
                        "iconCategory," +
                        "magnitudeOfDelay," +
                        "events(description,code,iconCategory)," +
                        "from," +
                        "to," +
                        "lengthInMeters," +
                        "delayInSeconds," +
                        "roadNumbers," +
                        "timeValidity" +
                        ")" +
                        ")"
            )

            val responseCode = connection.responseCode

            val inputStream =
                if (responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

            val responseBody =
                inputStream?.use { stream ->
                    BufferedReader(
                        InputStreamReader(stream)
                    ).use { reader ->
                        reader.readText()
                    }
                } ?: ""

            Log.d(
                TAG,
                "TomTom HTTP $responseCode, bodyLength=${responseBody.length}"
            )

            if (responseCode !in 200..299) {
                throw Exception(
                    "TomTom HTTP $responseCode: $responseBody"
                )
            }

            if (responseBody.isBlank()) {
                Log.d(TAG, "TomTom returned an empty response body")
                return@withContext emptyList()
            }

            val incidents = parseIncidents(responseBody)

            Log.d(
                TAG,
                "Parsed ${incidents.size} TomTom incidents"
            )

            incidents.forEach { incident ->
                Log.d(
                    TAG,
                    "Incident id=${incident.id}, " +
                            "category=${incident.category}, " +
                            "severity=${incident.magnitudeOfDelay}, " +
                            "description=${incident.description}, " +
                            "lat=${incident.latitude}, " +
                            "lon=${incident.longitude}"
                )
            }

            incidents

        } finally {
            connection.disconnect()
        }
    }

    private fun parseIncidents(
        responseBody: String
    ): List<TomTomIncident> {

        val root = JSONObject(responseBody)

        val incidentsArray =
            root.optJSONArray("incidents")
                ?: return emptyList()

        val incidents = mutableListOf<TomTomIncident>()

        for (index in 0 until incidentsArray.length()) {
            val incident =
                incidentsArray.optJSONObject(index)
                    ?: continue

            val properties =
                incident.optJSONObject("properties")
                    ?: continue

            val id = properties.optString(
                "id",
                "incident_$index"
            )

            val category = properties.optString(
                "iconCategory",
                "unknown"
            )

            val magnitude = properties.optString(
                "magnitudeOfDelay",
                "unknown"
            )

            val events = properties.optJSONArray("events")

            val description =
                if (events != null && events.length() > 0) {
                    events.optJSONObject(0)?.optString(
                        "description",
                        ""
                    ) ?: ""
                } else {
                    ""
                }

            val roadNumbersArray =
                properties.optJSONArray("roadNumbers")

            val roadNumbers = mutableListOf<String>()

            if (roadNumbersArray != null) {
                for (roadIndex in 0 until roadNumbersArray.length()) {
                    val road = roadNumbersArray.optString(roadIndex)
                    if (road.isNotBlank()) {
                        roadNumbers.add(road)
                    }
                }
            }

            val delay =
                if (
                    properties.has("delayInSeconds") &&
                    !properties.isNull("delayInSeconds")
                ) {
                    properties.optInt("delayInSeconds")
                } else {
                    null
                }

            val length = properties.optDouble(
                "lengthInMeters",
                0.0
            )

            val from =
                if (
                    properties.has("from") &&
                    !properties.isNull("from")
                ) {
                    properties.optString("from")
                } else {
                    null
                }

            val to =
                if (
                    properties.has("to") &&
                    !properties.isNull("to")
                ) {
                    properties.optString("to")
                } else {
                    null
                }

            val geometry = incident.optJSONObject("geometry")

            val point = extractRepresentativeCoordinate(
                geometry?.opt("coordinates")
            )

            incidents.add(
                TomTomIncident(
                    id = id,
                    category = category,
                    magnitudeOfDelay = magnitude,
                    description = description,
                    roadNumbers = roadNumbers,
                    delayInSeconds = delay,
                    lengthInMeters = length,
                    from = from,
                    to = to,
                    latitude = point?.second,
                    longitude = point?.first
                )
            )
        }

        return incidents
    }

    /**
     * Returns a representative Pair(longitude, latitude) from GeoJSON
     * coordinates. Handles:
     *   Point       -> [lon, lat]
     *   LineString  -> [[lon, lat], ...]
     *   Multi...    -> deeper nested arrays
     */
    private fun extractRepresentativeCoordinate(
        coordinatesValue: Any?
    ): Pair<Double, Double>? {
        val coordinates = coordinatesValue as? JSONArray
            ?: return null

        return findFirstCoordinatePair(coordinates)
    }

    private fun findFirstCoordinatePair(
        array: JSONArray
    ): Pair<Double, Double>? {
        if (array.length() >= 2 &&
            array.opt(0) is Number &&
            array.opt(1) is Number
        ) {
            val longitude = array.optDouble(0, Double.NaN)
            val latitude = array.optDouble(1, Double.NaN)

            if (
                longitude.isFinite() &&
                latitude.isFinite() &&
                longitude in -180.0..180.0 &&
                latitude in -90.0..90.0
            ) {
                return longitude to latitude
            }
        }

        for (index in 0 until array.length()) {
            val nested = array.optJSONArray(index)
                ?: continue

            val result = findFirstCoordinatePair(nested)
            if (result != null) {
                return result
            }
        }

        return null
    }
}
