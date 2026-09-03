package com.saferoute.ai.vehicle

import android.content.Context
import com.saferoute.ai.model.VehicleProfile

// ============================================================

private const val VEHICLE_PREFS = "saferoute_vehicle"
private const val KEY_TYPE = "vehicle_type"
private const val KEY_MAKE = "make"
private const val KEY_MODEL = "model"
private const val KEY_YEAR = "year"
private const val KEY_FUEL = "fuel"
private const val KEY_SAVED = "saved"

fun loadVehicleProfile(context: Context): VehicleProfile {
    val prefs = context.getSharedPreferences(
        VEHICLE_PREFS,
        Context.MODE_PRIVATE
    )

    if (!prefs.getBoolean(KEY_SAVED, false)) {
        return VehicleProfile()
    }

    return VehicleProfile(
        vehicleType = prefs.getString(KEY_TYPE, "Car") ?: "Car",
        make = prefs.getString(KEY_MAKE, "") ?: "",
        model = prefs.getString(KEY_MODEL, "") ?: "",
        year = prefs.getString(KEY_YEAR, "") ?: "",
        fuel = prefs.getString(KEY_FUEL, "Petrol") ?: "Petrol"
    )
}

fun saveVehicleProfile(
    context: Context,
    vehicle: VehicleProfile
) {
    context.getSharedPreferences(
        VEHICLE_PREFS,
        Context.MODE_PRIVATE
    )
        .edit()
        .putBoolean(KEY_SAVED, true)
        .putString(KEY_TYPE, vehicle.vehicleType)
        .putString(KEY_MAKE, vehicle.make)
        .putString(KEY_MODEL, vehicle.model)
        .putString(KEY_YEAR, vehicle.year)
        .putString(KEY_FUEL, vehicle.fuel)
        .apply()
}

// ============================================================
