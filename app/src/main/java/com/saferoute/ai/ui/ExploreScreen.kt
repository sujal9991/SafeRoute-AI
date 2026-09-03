package com.saferoute.ai.ui

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

    var showSearch by remember {
        mutableStateOf(openSearchInitially)
    }

    val scope =
        androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(openSearchInitially) {
        if (openSearchInitially) {
            showSearch = true
            onSearchOpened()
        }
    }

    LaunchedEffect(
        currentLocation,
        destination
    ) {
        if (
            currentLocation == null ||
            destination == null
        ) {
            return@LaunchedEffect
        }

        routeLoading = true
        routeError = null
        routes = emptyList()
        selectedRoute = 0

        try {
            val calculatedRoutes =
                withContext(Dispatchers.IO) {
                    calculateOsrmRoutes(
                        start = currentLocation,
                        destination = GeoPoint(
                            destination.latitude,
                            destination.longitude
                        )
                    )
                }

            routes = calculatedRoutes

            if (calculatedRoutes.isEmpty()) {
                routeError =
                    "No route was found."
            }

        } catch (e: Exception) {
            routeError =
                "Unable to calculate route:\n${e.message ?: "Unknown network error"}"
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
                isNavigating = isNavigating
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
                                    searchResults =
                                        emptyList()
                                    routeError = null
                                }
                            },

                            modifier =
                                Modifier.fillMaxWidth(),

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
                        onRouteSelected = {
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
                NavigationPanel(
                    route = routes.getOrNull(selectedRoute),
                    stepIndex = navigationStepIndex,
                    distanceRemaining = navigationDistanceRemaining,
                    arrived = navigationArrived,
                    onEndNavigation = {
                        isNavigating = false
                        navigationArrived = false
                        navigationStepIndex = 0
                    }
                )
            }
        }
    }
}

// ============================================================
// NAVIGATION PANEL
// ============================================================

@Composable
fun NavigationPanel(
    route: RouteOption?,
    stepIndex: Int,
    distanceRemaining: Double,
    arrived: Boolean,
    onEndNavigation: () -> Unit
) {
    val safeStepIndex =
        if (route != null && route.steps.isNotEmpty()) {
            stepIndex.coerceIn(0, route.steps.lastIndex)
        } else {
            0
        }

    val instruction =
        if (arrived) {
            "You have arrived at your destination"
        } else if (route != null && route.steps.isNotEmpty()) {
            route.steps[safeStepIndex].instruction
        } else {
            "Continue on the selected route"
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceColor
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    tint = Green,
                    modifier = Modifier.size(30.dp)
                )

                Spacer(
                    modifier = Modifier.width(12.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (arrived) "ARRIVED" else "NAVIGATING",
                        color = Green,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = instruction,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Remaining",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )

                    Text(
                        text = formatDistance(distanceRemaining),
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!arrived) {
                    Text(
                        text = "Follow the green route",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Button(
                onClick = onEndNavigation,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceLight,
                    contentColor = TextPrimary
                )
            ) {
                Text(
                    text = if (arrived) "Done" else "End navigation",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
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
        kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(latitude1)) *
                kotlin.math.cos(Math.toRadians(latitude2)) *
                kotlin.math.sin(dLon / 2) *
                kotlin.math.sin(dLon / 2)

    val c =
        2.0 * kotlin.math.atan2(
            kotlin.math.sqrt(a),
            kotlin.math.sqrt(1.0 - a)
        )

    return earthRadius * c
}

// ============================================================
// ============================================================

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
    onRouteSelected: (Int) -> Unit
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

                            Text(
                                text =
                                    "${formatDistance(route.distanceMeters)} • " +
                                            formatDuration(
                                                route.durationSeconds
                                            ),
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
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
        }
    }
}

// ============================================================
