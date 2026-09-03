package com.saferoute.ai.navigation

import com.saferoute.ai.model.*
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import com.saferoute.ai.navigation.httpGetText
import com.saferoute.ai.navigation.buildInstruction

// ============================================================

fun calculateOsrmRoutes(
    start: GeoPoint,
    destination: GeoPoint
): List<RouteOption> {

    val coordinateString =
        "${start.longitude},${start.latitude};" +
                "${destination.longitude},${destination.latitude}"

    val urls = listOf(
        "https://router.project-osrm.org/route/v1/driving/" +
                coordinateString +
                "?alternatives=true" +
                "&steps=true" +
                "&geometries=geojson" +
                "&overview=full",

        "https://routing.openstreetmap.de/routed-car/route/v1/driving/" +
                coordinateString +
                "?alternatives=true" +
                "&steps=true" +
                "&geometries=geojson" +
                "&overview=full"
    )

    var lastError =
        "No routing server responded."

    for (urlString in urls) {

        try {

            val response =
                httpGetText(
                    urlString = urlString,
                    timeoutMillis = 20000
                )

            val root =
                JSONObject(response)

            val code =
                root.optString("code")

            if (code != "Ok") {

                val message =
                    root.optString(
                        "message",
                        "OSRM returned $code"
                    )

                lastError =
                    "Routing server: $message"

                continue
            }

            val routeArray =
                root.optJSONArray("routes")
                    ?: throw Exception(
                        "Routing response did not contain routes."
                    )

            val result =
                mutableListOf<RouteOption>()

            for (
                i in 0 until routeArray.length()
            ) {

                val route =
                    routeArray.getJSONObject(i)

                val distance =
                    route.optDouble(
                        "distance",
                        0.0
                    )

                val duration =
                    route.optDouble(
                        "duration",
                        0.0
                    )

                val geometry =
                    route
                        .getJSONObject("geometry")
                        .getJSONArray("coordinates")

                val points =
                    mutableListOf<GeoPoint>()

                for (
                    j in 0 until geometry.length()
                ) {

                    val coordinate =
                        geometry.getJSONArray(j)

                    points.add(
                        GeoPoint(
                            coordinate.getDouble(1),
                            coordinate.getDouble(0)
                        )
                    )
                }

                val steps =
                    mutableListOf<RouteStep>()

                val legs =
                    route.optJSONArray("legs")
                        ?: JSONArray()

                for (
                    legIndex in 0 until legs.length()
                ) {

                    val leg =
                        legs.getJSONObject(
                            legIndex
                        )

                    val stepArray =
                        leg.optJSONArray("steps")
                            ?: JSONArray()

                    for (
                        stepIndex in 0 until stepArray.length()
                    ) {

                        val step =
                            stepArray.getJSONObject(
                                stepIndex
                            )

                        val maneuver =
                            step.optJSONObject(
                                "maneuver"
                            )

                        steps.add(
                            RouteStep(
                                instruction =
                                    buildInstruction(
                                        step,
                                        maneuver
                                    ),
                                distanceMeters =
                                    step.optDouble(
                                        "distance",
                                        0.0
                                    )
                            )
                        )
                    }
                }

                result.add(
                    RouteOption(
                        distanceMeters = distance,
                        durationSeconds = duration,
                        geometry = points,
                        steps = steps
                    )
                )
            }

            if (result.isNotEmpty()) {
                return result
            }

            lastError =
                "Routing server returned no routes."

        } catch (e: Exception) {

            lastError =
                e.message
                    ?: "Unknown routing network error."
        }
    }

    throw Exception(lastError)
}

// ============================================================
