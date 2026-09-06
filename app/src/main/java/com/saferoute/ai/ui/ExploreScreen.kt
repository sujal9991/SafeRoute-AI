
package com.saferoute.ai.ui

import android.speech.tts.TextToSpeech
import android.util.Log

import androidx.activity.compose.BackHandler

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import java.time.LocalTime
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.roundToInt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saferoute.ai.core.*
import com.saferoute.ai.map.SafeRouteMap
import com.saferoute.ai.model.*
import com.saferoute.ai.navigation.calculateOsrmRoutes
import com.saferoute.ai.util.formatDistance
import com.saferoute.ai.util.formatDuration
import com.saferoute.ai.search.searchNominatim
import com.saferoute.ai.traffic.TomTomIncident
import com.saferoute.ai.traffic.TomTomTrafficService
import com.saferoute.ai.traffic.TomTomTrafficFlowService
import com.saferoute.ai.traffic.TrafficFlowResult
import com.saferoute.ai.weather.WeatherResult
import com.saferoute.ai.weather.WeatherService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint

// ============================================================

@Composable
fun ExploreScreen(
    currentLocation: GeoPoint?,
    destination: SearchResult?,
    openSearchInitially: Boolean,
    onSearchOpened: () -> Unit,
    onDestinationSelected: (SearchResult) -> Unit,
    onDestinationCleared: () -> Unit,
    onBack: () -> Unit
) {
    var searchText by remember {
        mutableStateOf("")
    }

    var searchResults by remember {
        mutableStateOf<List<SearchResult>>(
            emptyList()
        )
    }

    var searching by remember {
        mutableStateOf(false)
    }

    var routeLoading by remember {
        mutableStateOf(false)
    }

    var routeError by remember {
        mutableStateOf<String?>(null)
    }

    var routes by remember {
        mutableStateOf<List<RouteOption>>(
            emptyList()
        )
    }

    var selectedRoute by remember {
        mutableStateOf(0)
    }

    var isNavigating by remember {
        mutableStateOf(false)
    }

    var navigationStepIndex by remember {
        mutableStateOf(0)
    }

    var navigationDistanceRemaining by remember {
        mutableStateOf(0.0)
    }

    var navigationArrived by remember {
        mutableStateOf(false)
    }

    var navigationMuted by remember {
        mutableStateOf(false)
    }

    var textToSpeech by remember {
        mutableStateOf<TextToSpeech?>(null)
    }

    var ttsReady by remember {
        mutableStateOf(false)
    }

    var recenterRequest by remember {
        mutableStateOf(0)
    }

    // ------------------------------------------------------------
    // TOMTOM LIVE TRAFFIC INCIDENTS
    // ------------------------------------------------------------

    var trafficIncidents by remember {
        mutableStateOf<List<TomTomIncident>>(emptyList())
    }

    var trafficLoading by remember {
        mutableStateOf(false)
    }

    var trafficError by remember {
        mutableStateOf<String?>(null)
    }

    // ------------------------------------------------------------
    // TOMTOM LIVE TRAFFIC FLOW / ETA
    // ------------------------------------------------------------

    var trafficFlow by remember {
        mutableStateOf<TrafficFlowResult?>(null)
    }

    var trafficFlowLoading by remember {
        mutableStateOf(false)
    }

    var trafficFlowError by remember {
        mutableStateOf<String?>(null)
    }

    // ------------------------------------------------------------
    // WEATHER ALONG ROUTE
    // ------------------------------------------------------------

    var routeWeather by remember {
        mutableStateOf<WeatherResult?>(null)
    }

    var weatherLoading by remember {
        mutableStateOf(false)
    }

    var weatherError by remember {
        mutableStateOf<String?>(null)
    }

    var showSearch by remember {
        mutableStateOf(openSearchInitially)
    }

    val scope =
        rememberCoroutineScope()

    val context =
        androidx.compose.ui.platform.LocalContext.current

    // ------------------------------------------------------------
    // ANDROID SYSTEM BACK / EDGE-SWIPE
    // ------------------------------------------------------------
    //
    // Android's gesture-navigation edge swipe triggers this BackHandler.
    // Handle Back according to the current Explore state:
    //
    // 1. Navigation -> route preview
    // 2. Search -> close search
    // 3. Destination/routes -> clear route and stay on Explore
    // 4. Normal Explore -> Home
    //
    BackHandler {
        when {
            isNavigating -> {
                // Exit navigation but keep the selected route visible.
                isNavigating = false
                navigationArrived = false
                navigationStepIndex = 0
                navigationMuted = false
            }

            showSearch -> {
                // Close the search UI first.
                showSearch = false
                searchResults = emptyList()
                searching = false
                routeError = null
            }

            destination != null || routes.isNotEmpty() -> {
                // Clear the local Explore route state.
                routes = emptyList()
                selectedRoute = 0
                navigationStepIndex = 0
                navigationDistanceRemaining = 0.0
                navigationArrived = false
                navigationMuted = false

                trafficIncidents = emptyList()
                trafficFlow = null
                routeWeather = null

                trafficError = null
                trafficFlowError = null
                weatherError = null

                // Tell MainActivity to clear the actual destination.
                onDestinationCleared()
            }

            else -> {
                // Normal Explore screen: return to Home.
                onBack()
            }
        }
    }

    DisposableEffect(Unit) {
        val speaker = TextToSpeech(context, null)

        val result = speaker.setLanguage(Locale.US)
        speaker.setSpeechRate(0.95f)
        speaker.setPitch(1.0f)

        ttsReady =
            result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED

        textToSpeech = speaker

        onDispose {
            speaker.stop()
            speaker.shutdown()
            textToSpeech = null
            ttsReady = false
        }
    }

    LaunchedEffect(openSearchInitially) {
        if (openSearchInitially) {
            showSearch = true
            onSearchOpened()
        }
    }

    // Calculate routes only when the destination changes, or when the first GPS fix arrives.
    // Live GPS updates must NOT recalculate the route because that would reset an alternate
    // route selection back to the fastest route.
    LaunchedEffect(
        destination,
        currentLocation == null
    ) {
        if (destination == null || currentLocation == null) {
            return@LaunchedEffect
        }

        val routeStart = currentLocation
        val routeDestination = GeoPoint(
            destination.latitude,
            destination.longitude
        )

        routeLoading = true
        routeError = null
        routes = emptyList()
        selectedRoute = 0

        try {
            val calculatedRoutes =
                withContext(Dispatchers.IO) {
                    calculateOsrmRoutes(
                        start = routeStart,
                        destination = routeDestination
                    )
                }

            routes = calculatedRoutes

            if (calculatedRoutes.isEmpty()) {
                routeError = "No route was found."
            }
        } catch (e: Exception) {
            routeError =
                "Unable to calculate route: ${e.message ?: "Unknown network error"}"
        } finally {
            routeLoading = false
        }
    }

    LaunchedEffect(
        currentLocation,
        isNavigating,
        selectedRoute,
        routes
    ) {
        if (!isNavigating || routes.isEmpty()) {
            return@LaunchedEffect
        }

        val route = routes.getOrNull(selectedRoute)
            ?: return@LaunchedEffect

        if (currentLocation == null || route.geometry.isEmpty()) {
            return@LaunchedEffect
        }

        val nearestIndex = route.geometry.indices.minByOrNull { index ->
            distanceBetweenMeters(
                currentLocation.latitude,
                currentLocation.longitude,
                route.geometry[index].latitude,
                route.geometry[index].longitude
            )
        } ?: 0

        val distanceFromRouteEnd = distanceBetweenMeters(
            currentLocation.latitude,
            currentLocation.longitude,
            route.geometry.last().latitude,
            route.geometry.last().longitude
        )

        if (distanceFromRouteEnd <= 50.0) {
            navigationArrived = true
            navigationDistanceRemaining = 0.0
            return@LaunchedEffect
        }

        val progress =
            if (route.geometry.size <= 1) {
                0.0
            } else {
                nearestIndex.toDouble() /
                        (route.geometry.size - 1).toDouble()
            }

        val travelledDistance =
            route.distanceMeters * progress

        navigationDistanceRemaining =
            (route.distanceMeters - travelledDistance).coerceAtLeast(0.0)

        if (route.steps.isNotEmpty()) {
            var cumulative = 0.0
            var stepIndex = 0

            route.steps.forEachIndexed { index, step ->
                cumulative += step.distanceMeters
                if (travelledDistance >= cumulative) {
                    stepIndex = (index + 1)
                        .coerceAtMost(route.steps.lastIndex)
                }
            }

            navigationStepIndex = stepIndex
        }
    }

    // ------------------------------------------------------------
    // LIVE DESTINATION AUTOCOMPLETE
    // ------------------------------------------------------------

    LaunchedEffect(searchText, destination, showSearch) {
        if (
            destination != null ||
            !showSearch ||
            searchText.trim().length < 3
        ) {
            if (searchText.trim().length < 3) {
                searchResults = emptyList()
            }
            return@LaunchedEffect
        }

        kotlinx.coroutines.delay(650L)

        val query = searchText.trim()

        try {
            searching = true
            routeError = null

            val results = withContext(Dispatchers.IO) {
                searchNominatim(
                    query,
                    currentLocation
                )
            }

            // Ignore an older request if the user has already typed something else.
            if (query == searchText.trim()) {
                searchResults = results
            }
        } catch (e: Exception) {
            if (query == searchText.trim()) {
                searchResults = emptyList()
            }

            Log.e(
                "SafeRouteSearch",
                "Autocomplete failed",
                e
            )
        } finally {
            if (query == searchText.trim()) {
                searching = false
            }
        }
    }

    // ------------------------------------------------------------
    // TOMTOM LIVE TRAFFIC INCIDENTS
    // ------------------------------------------------------------

    LaunchedEffect(routes, selectedRoute, isNavigating) {
        if (routes.isEmpty()) {
            trafficIncidents = emptyList()
            trafficError = null
            trafficLoading = false
            return@LaunchedEffect
        }

        while (true) {
            val route = routes.getOrNull(selectedRoute)

            if (route == null || route.geometry.isEmpty()) {
                trafficIncidents = emptyList()
                trafficError = null
                trafficLoading = false
                return@LaunchedEffect
            }

            val routeMinLatitude = route.geometry.minOf { it.latitude }
            val routeMaxLatitude = route.geometry.maxOf { it.latitude }
            val routeMinLongitude = route.geometry.minOf { it.longitude }
            val routeMaxLongitude = route.geometry.maxOf { it.longitude }

            // Query a slightly wider corridor around the complete selected
            // route. This catches incidents just beside the road instead of
            // requiring the incident geometry to lie exactly on the route.
            val latitudePadding = 0.03
            val longitudePadding = 0.03

            val minLatitude = (routeMinLatitude - latitudePadding).coerceIn(-90.0, 90.0)
            val maxLatitude = (routeMaxLatitude + latitudePadding).coerceIn(-90.0, 90.0)
            val minLongitude = (routeMinLongitude - longitudePadding).coerceIn(-180.0, 180.0)
            val maxLongitude = (routeMaxLongitude + longitudePadding).coerceIn(-180.0, 180.0)

            trafficLoading = true
            trafficError = null

            try {
                val result = withContext(Dispatchers.IO) {
                    TomTomTrafficService.getIncidents(
                        minLongitude = minLongitude,
                        minLatitude = minLatitude,
                        maxLongitude = maxLongitude,
                        maxLatitude = maxLatitude
                    )
                }

                trafficIncidents = result

                Log.d(
                    "SafeRouteTomTom",
                    "ExploreScreen received ${result.size} incidents"
                )
            } catch (e: Exception) {
                trafficIncidents = emptyList()
                trafficError =
                    e.message ?: "Unable to load live traffic."

                Log.e(
                    "SafeRouteTomTom",
                    "Traffic request failed",
                    e
                )
            } finally {
                trafficLoading = false
            }

            // Before navigation, one current snapshot is enough for route
            // comparison. During navigation refresh every five minutes.
            if (!isNavigating) {
                return@LaunchedEffect
            }

            kotlinx.coroutines.delay(5 * 60 * 1000L)
        }
    }

    // ------------------------------------------------------------
    // TOMTOM LIVE TRAFFIC FLOW / TRAFFIC-AWARE ETA
    // ------------------------------------------------------------

    LaunchedEffect(routes, selectedRoute, isNavigating) {
        if (routes.isEmpty()) {
            trafficFlow = null
            trafficFlowError = null
            trafficFlowLoading = false
            return@LaunchedEffect
        }

        while (true) {
            val route = routes.getOrNull(selectedRoute)

            if (route == null || route.geometry.size < 2) {
                trafficFlow = null
                trafficFlowError = null
                trafficFlowLoading = false
                return@LaunchedEffect
            }

            trafficFlowLoading = true
            trafficFlowError = null

            try {
                val result = withContext(Dispatchers.IO) {
                    TomTomTrafficFlowService.analyzeRoute(route.geometry)
                }

                trafficFlow = result

                Log.d(
                    "SafeRouteTomTomFlow",
                    "Flow: level=${result.trafficLevel}, " +
                            "relative=${result.averageRelativeSpeed}, " +
                            "delay=${result.estimatedDelayFraction}, " +
                            "matched=${result.matchedSegments}"
                )
            } catch (e: Exception) {
                trafficFlow = null
                trafficFlowError =
                    e.message ?: "Unable to load traffic flow."

                Log.e(
                    "SafeRouteTomTomFlow",
                    "Traffic flow request failed",
                    e
                )
            } finally {
                trafficFlowLoading = false
            }

            // Before navigation, one snapshot is enough for route comparison.
            // During navigation, refresh the traffic-adjusted ETA every five minutes.
            if (!isNavigating) {
                return@LaunchedEffect
            }

            kotlinx.coroutines.delay(5 * 60 * 1000L)
        }
    }

    // ------------------------------------------------------------
    // WEATHER ALONG ROUTE
    // ------------------------------------------------------------

    LaunchedEffect(routes, selectedRoute, isNavigating) {
        Log.d(
            "SafeRouteWeather",
            "Weather effect started: routes=${routes.size}, selectedRoute=$selectedRoute, navigating=$isNavigating"
        )

        if (routes.isEmpty()) {
            Log.d("SafeRouteWeather", "No routes yet - clearing weather state")
            routeWeather = null
            weatherError = null
            weatherLoading = false
            return@LaunchedEffect
        }

        while (true) {
            val route = routes.getOrNull(selectedRoute)

            if (route == null || route.geometry.isEmpty()) {
                Log.d("SafeRouteWeather", "Selected route is unavailable or empty")
                routeWeather = null
                weatherError = null
                weatherLoading = false
                return@LaunchedEffect
            }

            // Use the route midpoint as the first representative weather sample.
            val samplePoint = route.geometry[route.geometry.size / 2]

            Log.d(
                "SafeRouteWeather",
                "Requesting weather at lat=${samplePoint.latitude}, lon=${samplePoint.longitude}"
            )

            weatherLoading = true
            weatherError = null

            try {
                val result = withContext(Dispatchers.IO) {
                    WeatherService.getWeather(
                        latitude = samplePoint.latitude,
                        longitude = samplePoint.longitude
                    )
                }

                routeWeather = result

                Log.d(
                    "SafeRouteWeather",
                    "Weather received: ${result.description}, " +
                            "${result.temperatureC}°C, " +
                            "rain=${result.precipitationMm}mm, " +
                            "wind=${result.windSpeedKph}km/h, " +
                            "severe=${result.isSevere}"
                )
            } catch (e: Exception) {
                routeWeather = null
                weatherError = e.message ?: "Unable to load weather."

                Log.e(
                    "SafeRouteWeather",
                    "Weather request failed",
                    e
                )
            } finally {
                weatherLoading = false
            }

            // One snapshot is enough before navigation.
            // During navigation refresh every ten minutes.
            if (!isNavigating) {
                Log.d("SafeRouteWeather", "Weather loaded; navigation is not active")
                return@LaunchedEffect
            }

            kotlinx.coroutines.delay(10 * 60 * 1000L)
        }
    }

    // ------------------------------------------------------------
    // SPOKEN TURN-BY-TURN NAVIGATION
    // ------------------------------------------------------------

    LaunchedEffect(
        isNavigating,
        navigationStepIndex,
        navigationMuted,
        ttsReady,
        selectedRoute
    ) {
        if (
            !isNavigating ||
            navigationMuted ||
            !ttsReady
        ) {
            return@LaunchedEffect
        }

        val route = routes.getOrNull(selectedRoute)
            ?: return@LaunchedEffect

        val step = route.steps.getOrNull(navigationStepIndex)
            ?: return@LaunchedEffect

        val instruction = step.instruction.trim()

        if (instruction.isBlank()) {
            return@LaunchedEffect
        }

        val distanceText = formatDistance(
            step.distanceMeters
        )

        val spokenText =
            if (step.distanceMeters > 0.0) {
                "$instruction in approximately $distanceText."
            } else {
                instruction
            }

        textToSpeech?.speak(
            spokenText,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "navigation_step_$navigationStepIndex"
        )
    }

    Scaffold(
        containerColor = Background
    ) { paddingValues ->

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
        ) {

            SafeRouteMap(
                currentLocation = currentLocation,
                destination = destination,
                routes = routes,
                selectedRoute = selectedRoute,
                isNavigating = isNavigating,
                recenterRequest = recenterRequest
            )

            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                color =
                    Background.copy(
                        alpha = 0.96f
                    ),
                shape =
                    RoundedCornerShape(18.dp)
            ) {

                Column(
                    modifier =
                        Modifier.padding(10.dp)
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        IconButton(
                            onClick = onBack
                        ) {
                            Icon(
                                imageVector =
                                    Icons.Default.ArrowBack,
                                contentDescription =
                                    "Back",
                                tint = TextPrimary
                            )
                        }

                        OutlinedTextField(
                            value =
                                if (destination != null) {
                                    destination.displayName
                                } else {
                                    searchText
                                },

                            onValueChange = {
                                if (destination == null) {
                                    searchText = it
                                    routeError = null

                                    if (it.trim().isBlank()) {
                                        searchResults = emptyList()
                                    }
                                }
                            },

                            modifier =
                                Modifier.weight(1f),

                            enabled =
                                destination == null,

                            placeholder = {
                                Text(
                                    text =
                                        "Where do you want to go?",
                                    color =
                                        TextTertiary
                                )
                            },

                            leadingIcon = {
                                Icon(
                                    imageVector =
                                        Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Green
                                )
                            },

                            singleLine = true,
                            shape =
                                RoundedCornerShape(14.dp)
                        )


                        if (
                            routes.isNotEmpty() &&
                            !isNavigating &&
                            (trafficLoading ||
                                    trafficIncidents.isNotEmpty() ||
                                    trafficError != null)
                        ) {
                            Spacer(modifier = Modifier.height(6.dp))

                            TrafficIncidentPill(
                                incidents = trafficIncidents,
                                loading = trafficLoading,
                                error = trafficError
                            )
                        }

                    }

                    if (
                        routes.isNotEmpty() &&
                        !isNavigating &&
                        (weatherLoading ||
                                routeWeather != null ||
                                weatherError != null)
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        WeatherPill(
                            weather = routeWeather,
                            loading = weatherLoading,
                            error = weatherError,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (
                        destination == null &&
                        showSearch
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        Button(
                            onClick = {

                                if (
                                    searchText.isBlank() ||
                                    searching
                                ) {
                                    return@Button
                                }

                                searchResults =
                                    emptyList()
                                routeError = null
                                searching = true

                                scope.launch {
                                    try {
                                        val results =
                                            withContext(
                                                Dispatchers.IO
                                            ) {
                                                searchNominatim(
                                                    searchText,
                                                    currentLocation
                                                )
                                            }

                                        searchResults =
                                            results

                                        if (results.isEmpty()) {
                                            routeError =
                                                "No destination found."
                                        }

                                    } catch (e: Exception) {
                                        routeError =
                                            "Search failed:\n${e.message ?: "Check your internet connection."}"
                                    } finally {
                                        searching = false
                                    }
                                }
                            },

                            modifier =
                                Modifier.fillMaxWidth(),

                            shape =
                                RoundedCornerShape(13.dp),

                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = Green,
                                    contentColor = Background
                                )
                        ) {

                            if (searching) {
                                CircularProgressIndicator(
                                    modifier =
                                        Modifier.size(18.dp),
                                    color = Background,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text =
                                        "Search destination",
                                    fontWeight =
                                        FontWeight.Bold
                                )
                            }
                        }

                        if (searchResults.isNotEmpty()) {

                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )

                            searchResults.forEach { result ->

                                SearchResultItem(
                                    result = result,
                                    onClick = {
                                        onDestinationSelected(
                                            result
                                        )

                                        showSearch = false
                                        searchResults =
                                            emptyList()
                                    }
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(6.dp)
                                )
                            }
                        }
                    }

                    if (destination != null) {

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        Button(
                            onClick = {

                                showSearch = true
                                searchText = ""
                                searchResults =
                                    emptyList()
                                routeError = null
                                routes = emptyList()
                            },

                            modifier =
                                Modifier.fillMaxWidth(),

                            shape =
                                RoundedCornerShape(13.dp),

                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor =
                                        SurfaceLight,
                                    contentColor =
                                        Green
                                )
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Search,
                                contentDescription = null
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(8.dp)
                            )

                            Text(
                                text =
                                    "Change destination"
                            )
                        }
                    }
                }
            }

            if (routeLoading) {

                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                    shape =
                        RoundedCornerShape(18.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = SurfaceColor
                        )
                ) {

                    Row(
                        modifier =
                            Modifier.padding(18.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        CircularProgressIndicator(
                            modifier =
                                Modifier.size(25.dp),
                            color = Green,
                            strokeWidth = 3.dp
                        )

                        Spacer(
                            modifier =
                                Modifier.width(14.dp)
                        )

                        Column {
                            Text(
                                text =
                                    "Calculating route",
                                color =
                                    TextPrimary,
                                fontWeight =
                                    FontWeight.SemiBold
                            )

                            Text(
                                text =
                                    "Finding available routes...",
                                color =
                                    TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            if (
                routeError != null &&
                !routeLoading
            ) {

                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                    shape =
                        RoundedCornerShape(18.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = SurfaceColor
                        )
                ) {

                    Column(
                        modifier =
                            Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = routeError!!,
                            color = Red,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            if (
                !routeLoading &&
                routes.isNotEmpty()
            ) {

                if (!isNavigating) {
                    RouteResultCard(
                        routes = routes,
                        selectedRoute = selectedRoute,
                        trafficFlow = trafficFlow,
                        trafficFlowLoading = trafficFlowLoading,
                        trafficFlowError = trafficFlowError,
                        routeWeather = routeWeather,
                        weatherLoading = weatherLoading,
                        weatherError = weatherError,
                        onRouteSelected = {
                            selectedRoute = it
                            navigationStepIndex = 0
                            navigationArrived = false
                            navigationDistanceRemaining =
                                routes[it].distanceMeters
                            isNavigating = false
                        },
                        onStartNavigation = {
                            selectedRoute = it
                            navigationStepIndex = 0
                            navigationArrived = false
                            navigationDistanceRemaining =
                                routes[it].distanceMeters
                            isNavigating = true
                        }
                    )
                }

            }

            if (isNavigating) {
                NavigationModeOverlay(
                    route = routes.getOrNull(selectedRoute),
                    trafficFlow = trafficFlow,
                    routeWeather = routeWeather,
                    weatherLoading = weatherLoading,
                    weatherError = weatherError,
                    stepIndex = navigationStepIndex,
                    distanceRemaining = navigationDistanceRemaining,
                    arrived = navigationArrived,
                    muted = navigationMuted,
                    recenterRequest = {
                        recenterRequest++
                    },
                    onMute = { navigationMuted = !navigationMuted },
                    onEndNavigation = {
                        isNavigating = false
                        navigationArrived = false
                        navigationStepIndex = 0
                        navigationMuted = false
                    }
                )
            }
        }
    }
}

// GOOGLE-STYLE NAVIGATION OVERLAY
// ============================================================

@Composable
fun NavigationModeOverlay(
    route: RouteOption?,
    trafficFlow: TrafficFlowResult?,
    routeWeather: WeatherResult?,
    weatherLoading: Boolean,
    weatherError: String?,
    stepIndex: Int,
    recenterRequest: () -> Unit,
    distanceRemaining: Double,
    arrived: Boolean,
    muted: Boolean,
    onMute: () -> Unit,
    onEndNavigation: () -> Unit
) {
    val safeStepIndex =
        if (route != null && route.steps.isNotEmpty()) {
            stepIndex.coerceIn(0, route.steps.lastIndex)
        } else {
            0
        }

    val currentStep =
        route?.steps?.getOrNull(safeStepIndex)

    val nextStep =
        route?.steps?.getOrNull(safeStepIndex + 1)

    val trafficDelayFraction =
        trafficFlow?.estimatedDelayFraction?.coerceIn(0.0, 3.0) ?: 0.0

    val trafficAdjustedDurationSeconds =
        route?.durationSeconds?.toDouble()?.times(1.0 + trafficDelayFraction) ?: 0.0

    val remainingSeconds =
        if (route != null && route.distanceMeters > 0.0) {
            trafficAdjustedDurationSeconds *
                    (distanceRemaining / route.distanceMeters)
        } else {
            0.0
        }

    val eta = remember(remainingSeconds) {
        LocalTime.now()
            .plusSeconds(remainingSeconds.coerceAtLeast(0.0).toLong())
    }

    val etaText = String.format(
        Locale.getDefault(),
        "%d:%02d %s",
        eta.hour % 12.let { if (it == 0) 12 else it },
        eta.minute,
        if (eta.hour < 12) "AM" else "PM"
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // ----------------------------------------------------
        // BACK TO ROUTE PREVIEW
        // ----------------------------------------------------

        NavigationRoundButton(
            icon = Icons.Default.ArrowBack,
            contentDescription = "Back to route preview",
            onClick = onEndNavigation,
            size = 52.dp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 12.dp)
        )

        // ----------------------------------------------------
        // TOP MANEUVER CARD
        // ----------------------------------------------------

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 74.dp, end = 12.dp, top = 12.dp),
            color = Color(0xFF00695C),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(
                    start = 18.dp,
                    end = 18.dp,
                    top = 12.dp,
                    bottom = 14.dp
                )
            ) {
                if (arrived) {
                    Text(
                        text = "You have arrived",
                        color = Color.White,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Destination reached",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 15.sp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = maneuverSymbol(currentStep?.instruction),
                            color = Color.White,
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(72.dp)
                        )

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = formatDistance(
                                    currentStep?.distanceMeters ?: 0.0
                                ),
                                color = Color.White,
                                fontSize = 29.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = navigationInstruction(currentStep),
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (nextStep != null) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Then",
                                color = Color.White.copy(alpha = 0.78f),
                                fontSize = 14.sp,
                                modifier = Modifier.width(72.dp)
                            )

                            Text(
                                text = maneuverSymbol(nextStep.instruction),
                                color = Color.White.copy(alpha = 0.92f),
                                fontSize = 27.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.25f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (routeWeather?.isSevere == true) {
                                    Icons.Default.Warning
                                } else {
                                    Icons.Default.WbSunny
                                },
                                contentDescription = "Live route weather",
                                tint = if (routeWeather?.isSevere == true) Red else Green,
                                modifier = Modifier.size(17.dp)
                            )

                            Spacer(modifier = Modifier.width(7.dp))

                            Text(
                                text = when {
                                    weatherLoading -> "Updating weather..."
                                    weatherError != null -> "Weather unavailable"
                                    routeWeather == null -> "Weather pending"
                                    else -> "${routeWeather.description} • " +
                                            "${routeWeather.temperatureC.roundToInt()}°C • " +
                                            "Rain ${routeWeather.precipitationMm} mm • " +
                                            "Wind ${routeWeather.windSpeedKph.roundToInt()} km/h"
                                },
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            if (routeWeather?.isSevere == true) {
                                Text(
                                    text = "SEVERE",
                                    color = Red,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // ----------------------------------------------------
        // RIGHT-SIDE CONTROLS
        // ----------------------------------------------------

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NavigationRoundButton(
                icon = Icons.Default.MyLocation,
                contentDescription = "Recenter map",
                onClick = recenterRequest
            )

            NavigationRoundButton(
                icon = if (muted) {
                    Icons.Default.VolumeOff
                } else {
                    Icons.Default.VolumeUp
                },
                contentDescription = if (muted) {
                    "Unmute navigation"
                } else {
                    "Mute navigation"
                },
                onClick = onMute
            )
        }

        // ----------------------------------------------------
        // BOTTOM NAVIGATION BAR
        // ----------------------------------------------------

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = Color.Black,
            shape = RoundedCornerShape(
                topStart = 26.dp,
                topEnd = 26.dp
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 18.dp,
                        vertical = 18.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavigationRoundButton(
                    icon = Icons.Default.Close,
                    contentDescription = "End navigation",
                    onClick = onEndNavigation,
                    size = 58.dp
                )

                Spacer(modifier = Modifier.width(18.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (arrived) {
                            "Arrived"
                        } else {
                            formatDuration(remainingSeconds)
                        },
                        color = Green,
                        fontSize = 29.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = if (arrived) {
                            "Destination reached"
                        } else {
                            "${formatDistance(distanceRemaining)}  •  $etaText"
                        },
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(18.dp))

                Surface(
                    modifier = Modifier.size(58.dp),
                    shape = RoundedCornerShape(50),
                    color = SurfaceLight
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Green,
                            modifier = Modifier.size(27.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationRoundButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    size: Dp = 54.dp,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .size(size)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = Color.Black.copy(alpha = 0.90f),
        border = BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.12f)
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(27.dp)
            )
        }
    }
}

private fun navigationInstruction(step: RouteStep?): String {
    if (step == null || step.instruction.isBlank()) {
        return "Continue on the route"
    }

    return step.instruction
}

private fun maneuverSymbol(instruction: String?): String {
    val text = instruction?.lowercase().orEmpty()

    return when {
        "u-turn" in text || "uturn" in text -> "↶"
        "sharp left" in text -> "↙"
        "sharp right" in text -> "↘"
        "left" in text -> "↰"
        "right" in text -> "↱"
        "roundabout" in text -> "⟳"
        "straight" in text || "continue" in text -> "↑"
        else -> "↑"
    }
}

// ============================================================
// DISTANCE HELPER
// ============================================================

private fun distanceBetweenMeters(
    latitude1: Double,
    longitude1: Double,
    latitude2: Double,
    longitude2: Double
): Double {
    val earthRadius = 6_371_000.0

    val dLat = Math.toRadians(latitude2 - latitude1)
    val dLon = Math.toRadians(longitude2 - longitude1)

    val a =
        sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(latitude1)) *
                cos(Math.toRadians(latitude2)) *
                sin(dLon / 2) *
                sin(dLon / 2)

    val c =
        2.0 * atan2(
            sqrt(a),
            sqrt(1.0 - a)
        )

    return earthRadius * c
}

// ============================================================
// ============================================================

@Composable
private fun WeatherPill(
    weather: WeatherResult?,
    loading: Boolean,
    error: String?,
    modifier: Modifier = Modifier
) {
    val pillText = when {
        loading -> "Checking weather"
        error != null -> "Weather unavailable"
        weather == null -> "Weather pending"
        else -> "${weather.description} • ${weather.temperatureC.roundToInt()}°C"
    }

    val iconTint = when {
        loading -> Green
        error != null -> TextSecondary
        weather?.isSevere == true -> Red
        else -> Green
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(50),
        color = Color.Black.copy(alpha = 0.90f),
        border = BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.14f)
        ),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 14.dp,
                    vertical = 9.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(15.dp),
                    color = Green,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = "Weather",
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(7.dp))

            Text(
                text = pillText,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TrafficIncidentPill(
    incidents: List<TomTomIncident>,
    loading: Boolean,
    error: String?
) {
    val severeCount = incidents.count {
        it.magnitudeOfDelay.equals("major", ignoreCase = true) ||
                it.magnitudeOfDelay.equals("veryMajor", ignoreCase = true)
    }

    val pillText = when {
        loading -> "Checking traffic"
        error != null -> "Traffic unavailable"
        severeCount > 0 ->
            "$severeCount major incident${if (severeCount == 1) "" else "s"}"
        incidents.isNotEmpty() ->
            "${incidents.size} traffic incident${if (incidents.size == 1) "" else "s"}"
        else -> "Traffic clear"
    }

    val iconTint = when {
        loading -> Green
        error != null -> TextSecondary
        severeCount > 0 -> Red
        incidents.isNotEmpty() -> Color(0xFFFFB300)
        else -> Green
    }

    Surface(
        modifier = Modifier
            .wrapContentWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(50),
        color = Color.Black.copy(alpha = 0.90f),
        border = BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.14f)
        ),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 7.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(15.dp),
                    color = Green,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Traffic,
                    contentDescription = "Live traffic",
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(7.dp))

            Text(
                text = pillText,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SearchResultItem(
    result: SearchResult,
    onClick: () -> Unit
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onClick()
                },
        shape =
            RoundedCornerShape(14.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = SurfaceLight
            )
    ) {

        Row(
            modifier =
                Modifier.padding(13.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Icon(
                imageVector =
                    Icons.Default.LocationOn,
                contentDescription = null,
                tint = Green
            )

            Spacer(
                modifier =
                    Modifier.width(10.dp)
            )

            Text(
                text = result.displayName,
                color = TextPrimary,
                fontSize = 12.sp,
                maxLines = 3,
                overflow =
                    TextOverflow.Ellipsis
            )
        }
    }
}

// ============================================================
// ============================================================

@Composable
fun RouteResultCard(
    routes: List<RouteOption>,
    selectedRoute: Int,
    trafficFlow: TrafficFlowResult?,
    trafficFlowLoading: Boolean,
    trafficFlowError: String?,
    routeWeather: WeatherResult?,
    weatherLoading: Boolean,
    weatherError: String?,
    onRouteSelected: (Int) -> Unit,
    onStartNavigation: (Int) -> Unit
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
        shape =
            RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = SurfaceColor
            )
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp)
        ) {

            // --------------------------------------------------------
            // WEATHER FOR THE SELECTED ROUTE
            // --------------------------------------------------------

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = if (routeWeather?.isSevere == true) {
                    Red.copy(alpha = 0.14f)
                } else {
                    GreenDark.copy(alpha = 0.55f)
                },
                border = BorderStroke(
                    1.dp,
                    if (routeWeather?.isSevere == true) {
                        Red.copy(alpha = 0.55f)
                    } else {
                        Green.copy(alpha = 0.35f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 13.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (weatherLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Green,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (routeWeather?.isSevere == true) {
                                Icons.Default.Warning
                            } else {
                                Icons.Default.WbSunny
                            },
                            contentDescription = "Route weather",
                            tint = if (routeWeather?.isSevere == true) {
                                Red
                            } else {
                                Green
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = when {
                                weatherLoading -> "Checking route weather..."
                                weatherError != null -> "Weather unavailable"
                                routeWeather == null -> "Weather data pending"
                                else -> "Weather along route"
                            },
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = when {
                                weatherLoading -> "Getting current conditions"
                                weatherError != null -> weatherError
                                    ?: "Unable to load weather"
                                routeWeather == null -> "Waiting for weather data"
                                else -> "${routeWeather.description}  •  " +
                                        "${routeWeather.temperatureC.roundToInt()}°C  •  " +
                                        "Rain ${routeWeather.precipitationMm} mm  •  " +
                                        "Wind ${routeWeather.windSpeedKph.roundToInt()} km/h"
                            },
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (routeWeather?.isSevere == true) {
                        Text(
                            text = "SEVERE",
                            color = Red,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Text(
                text = "Available routes",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            routes.forEachIndexed { index, route ->

                val title =
                    if (index == 0) {
                        "Fastest"
                    } else {
                        "Alternative ${index}"
                    }

                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onRouteSelected(index)
                            },
                    shape =
                        RoundedCornerShape(14.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                if (index == selectedRoute) {
                                    GreenDark
                                } else {
                                    SurfaceLight
                                }
                        ),
                    border =
                        BorderStroke(
                            1.dp,
                            if (index == selectedRoute) {
                                Green
                            } else {
                                Border
                            }
                        )
                ) {

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(13.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Navigation,
                            contentDescription = null,
                            tint =
                                if (index == selectedRoute) {
                                    Green
                                } else {
                                    TextSecondary
                                }
                        )

                        Spacer(
                            modifier =
                                Modifier.width(10.dp)
                        )

                        Column {

                            Text(
                                text = title,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight =
                                    FontWeight.SemiBold
                            )

                            val routeTrafficDelay =
                                if (index == selectedRoute) {
                                    trafficFlow?.estimatedDelayFraction
                                        ?.coerceIn(0.0, 3.0) ?: 0.0
                                } else {
                                    0.0
                                }

                            val trafficAdjustedDuration =
                                route.durationSeconds *
                                        (1.0 + routeTrafficDelay)

                            Text(
                                text =
                                    "${formatDistance(route.distanceMeters)} • " +
                                            formatDuration(
                                                trafficAdjustedDuration
                                            ),
                                color = TextSecondary,
                                fontSize = 11.sp
                            )

                            if (index == selectedRoute) {
                                val trafficText = when {
                                    trafficFlowLoading -> "Checking live traffic..."
                                    trafficFlowError != null -> "Traffic unavailable"
                                    trafficFlow == null -> "Traffic data pending"
                                    else -> {
                                        val delayMinutes =
                                            ((trafficAdjustedDuration -
                                                    route.durationSeconds) / 60.0)
                                                .roundToInt()
                                                .coerceAtLeast(0)

                                        val delayText =
                                            if (delayMinutes > 0) {
                                                " • +${delayMinutes} min"
                                            } else {
                                                ""
                                            }

                                        "Live traffic: ${trafficFlow.trafficLevel.lowercase()}" +
                                                delayText
                                    }
                                }

                                Text(
                                    text = trafficText,
                                    color = when {
                                        trafficFlow?.trafficLevel == "HEAVY" ||
                                                trafficFlow?.trafficLevel == "CLOSED" -> Red
                                        trafficFlow?.trafficLevel == "MODERATE" -> Color(0xFFFFB300)
                                        else -> Green
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                if (index < routes.lastIndex) {
                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )

            Divider(
                color = Border
            )

            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )

            val safeIndex =
                selectedRoute.coerceIn(
                    0,
                    routes.lastIndex
                )

            val route =
                routes[safeIndex]

            Text(
                text = "Turn-by-turn",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            route.steps
                .take(5)
                .forEachIndexed { index, step ->

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    vertical = 5.dp
                                ),
                        verticalAlignment =
                            Alignment.Top
                    ) {

                        Text(
                            text =
                                "${index + 1}",
                            color = Green,
                            fontSize = 12.sp,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            modifier =
                                Modifier.width(10.dp)
                        )

                        Column {

                            Text(
                                text =
                                    step.instruction,
                                color = TextPrimary,
                                fontSize = 12.sp
                            )

                            Text(
                                text =
                                    formatDistance(
                                        step.distanceMeters
                                    ),
                                color =
                                    TextTertiary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onStartNavigation(safeIndex)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Green,
                    contentColor = Background
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Start Navigation",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ============================================================

