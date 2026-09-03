package com.saferoute.ai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.saferoute.ai.core.SafeRouteTheme
import com.saferoute.ai.model.SearchResult
import com.saferoute.ai.model.VehicleProfile
import com.saferoute.ai.ui.ExploreScreen
import com.saferoute.ai.ui.HomeScreen
import com.saferoute.ai.ui.ProfileScreen
import com.saferoute.ai.ui.VehicleScreen
import com.saferoute.ai.vehicle.loadVehicleProfile
import com.saferoute.ai.vehicle.saveVehicleProfile
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint

// ============================================================
// MAIN ACTIVITY
// ============================================================

class MainActivity : ComponentActivity() {

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private var fallbackLocation by mutableStateOf<GeoPoint?>(null)

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val locationGranted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (locationGranted) {
                startLocationService()
                getInitialLocation()
            } else {
                Toast.makeText(
                    this,
                    "Location permission is required for SafeRoute.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().userAgentValue =
            "SafeRouteAI/1.0 Android navigation app"

        requestLocationPermission()
        getSavedLocation()

        setContent {
            SafeRouteTheme {

                val serviceLocation by
                LocationService.locationFlow.collectAsState()

                val currentLocation =
                    serviceLocation ?: fallbackLocation

                SafeRouteApp(
                    currentLocation = currentLocation
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (hasLocationPermission()) {
            startLocationService()
            getInitialLocation()
        }
    }

    // ========================================================
    // LOCATION PERMISSIONS
    // ========================================================

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {

        if (hasLocationPermission()) {
            startLocationService()
            getInitialLocation()
            return
        }

        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            permissions.add(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }

        permissionLauncher.launch(
            permissions.toTypedArray()
        )
    }

    // ========================================================
    // LOCATION SERVICE
    // ========================================================

    private fun startLocationService() {

        val intent = android.content.Intent(
            this,
            LocationService::class.java
        )

        ContextCompat.startForegroundService(
            this,
            intent
        )
    }

    // ========================================================
    // SAVED LOCATION
    // ========================================================

    private fun getSavedLocation() {

        val prefs = getSharedPreferences(
            "saferoute_location",
            MODE_PRIVATE
        )

        val latitude =
            prefs.getString(
                "latitude",
                null
            )?.toDoubleOrNull()

        val longitude =
            prefs.getString(
                "longitude",
                null
            )?.toDoubleOrNull()

        if (latitude != null && longitude != null) {

            fallbackLocation = GeoPoint(
                latitude,
                longitude
            )
        }
    }

    // ========================================================
    // INITIAL LOCATION
    // ========================================================

    private fun getInitialLocation() {

        if (!hasLocationPermission()) {
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->

                if (location != null) {

                    fallbackLocation = GeoPoint(
                        location.latitude,
                        location.longitude
                    )
                }
            }
    }
}

// ============================================================
// SAFEROUTE APP
// ============================================================

@Composable
fun SafeRouteApp(
    currentLocation: GeoPoint?
) {

    val context =
        androidx.compose.ui.platform.LocalContext.current

    // ========================================================
    // TAB STATE
    // ========================================================

    var selectedTab by remember {
        mutableStateOf("Home")
    }

    // ========================================================
    // DESTINATION STATE
    // ========================================================

    var destination by remember {
        mutableStateOf<SearchResult?>(null)
    }

    var openSearch by remember {
        mutableStateOf(false)
    }

    // ========================================================
    // VEHICLE STATE
    // ========================================================

    var vehicle by remember {
        mutableStateOf(
            loadVehicleProfile(context)
        )
    }

    // ========================================================
    // SCREEN NAVIGATION
    // ========================================================

    when (selectedTab) {

        // ====================================================
        // HOME
        // ====================================================

        "Home" -> {

            HomeScreen(
                currentLocation = currentLocation,
                vehicle = vehicle,

                onSearchDestination = {
                    selectedTab = "Explore"
                    openSearch = true
                },

                onVehicle = {
                    selectedTab = "Vehicle"
                },

                onProfile = {
                    selectedTab = "Profile"
                },

                onExplore = {
                    selectedTab = "Explore"
                }
            )
        }

        // ====================================================
        // EXPLORE
        // ====================================================

        "Explore" -> {

            ExploreScreen(
                currentLocation = currentLocation,
                destination = destination,
                openSearchInitially = openSearch,

                onSearchOpened = {
                    openSearch = false
                },

                onDestinationSelected = {
                    destination = it
                    openSearch = false
                },

                onBack = {
                    selectedTab = "Home"
                }
            )
        }

        // ====================================================
        // VEHICLE
        // ====================================================

        "Vehicle" -> {

            VehicleScreen(
                vehicle = vehicle,

                onSave = { savedVehicle ->

                    saveVehicleProfile(
                        context,
                        savedVehicle
                    )

                    vehicle = savedVehicle

                    Toast.makeText(
                        context,
                        "Vehicle saved successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                },

                onBack = {
                    selectedTab = "Home"
                }
            )
        }

        // ====================================================
        // PROFILE
        // ====================================================

        "Profile" -> {

            ProfileScreen(
                onBack = {
                    selectedTab = "Home"
                }
            )
        }
    }
}