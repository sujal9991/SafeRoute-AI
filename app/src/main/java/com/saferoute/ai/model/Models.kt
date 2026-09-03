package com.saferoute.ai.model

import org.osmdroid.util.GeoPoint

// ============================================================

data class SearchResult(
    val displayName: String,
    val latitude: Double,
    val longitude: Double
)

data class RouteStep(
    val instruction: String,
    val distanceMeters: Double
)

data class RouteOption(
    val distanceMeters: Double,
    val durationSeconds: Double,
    val geometry: List<GeoPoint>,
    val steps: List<RouteStep>
)

data class VehicleProfile(
    val vehicleType: String = "Car",
    val make: String = "",
    val model: String = "",
    val year: String = "",
    val fuel: String = "Petrol"
) {
    fun isSaved(): Boolean {
        return make.isNotBlank() || model.isNotBlank() || year.isNotBlank()
    }

    fun displayName(): String {
        val name = listOf(make, model)
            .filter { it.isNotBlank() }
            .joinToString(" ")

        return if (name.isBlank()) vehicleType else name
    }
}

// ============================================================
