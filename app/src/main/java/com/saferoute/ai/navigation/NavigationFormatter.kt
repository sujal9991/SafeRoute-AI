package com.saferoute.ai.navigation

import org.json.JSONObject
import java.util.Locale

// ============================================================

fun buildInstruction(
    step: JSONObject,
    maneuver: JSONObject?
): String {

    val road =
        step.optString(
            "name"
        ).trim()

    val type =
        maneuver
            ?.optString("type")
            ?.lowercase(Locale.US)
            ?: ""

    val modifier =
        maneuver
            ?.optString("modifier")
            ?.lowercase(Locale.US)
            ?: ""

    val action =
        when {

            type == "depart" ->
                "Start navigation"

            type == "arrive" ->
                "Arrive at your destination"

            type == "roundabout" ->
                "Enter the roundabout"

            type == "turn" &&
                    modifier.contains("left") ->
                "Turn left"

            type == "turn" &&
                    modifier.contains("right") ->
                "Turn right"

            type == "turn" ->
                "Turn"

            type == "merge" ->
                "Merge"

            type == "fork" ->
                "Take the fork"

            type == "on ramp" ->
                "Take the ramp"

            type == "off ramp" ->
                "Take the exit"

            else ->
                "Continue"
        }

    return if (road.isNotBlank()) {
        "$action onto $road"
    } else {
        action
    }
}

// ============================================================
