package com.saferoute.ai.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import com.saferoute.ai.model.*

// ============================================================

@Composable
fun SafeRouteMap(
    currentLocation: GeoPoint?,
    destination: SearchResult?,
    routes: List<RouteOption>,
    selectedRoute: Int,
    isNavigating: Boolean = false
) {
    val context =
        androidx.compose.ui.platform.LocalContext.current

    val mapView =
        remember {

            MapView(context).apply {

                setTileSource(
                    TileSourceFactory.MAPNIK
                )

                setMultiTouchControls(
                    true
                )

                controller.setZoom(
                    15.0
                )
            }
        }

    LaunchedEffect(
        currentLocation,
        destination
    ) {
        if (
            currentLocation != null &&
            destination == null
        ) {
            mapView.controller.animateTo(
                currentLocation
            )
        }
    }

    LaunchedEffect(
        currentLocation,
        isNavigating
    ) {
        if (isNavigating && currentLocation != null) {
            mapView.controller.setZoom(17.0)
            mapView.controller.animateTo(currentLocation)
        }
    }

    LaunchedEffect(
        currentLocation,
        destination,
        routes,
        selectedRoute,
        isNavigating
    ) {

        mapView.overlays.clear()

        currentLocation?.let {

            val marker =
                Marker(mapView)

            marker.position = it
            marker.title = "You are here"

            marker.setAnchor(
                Marker.ANCHOR_CENTER,
                Marker.ANCHOR_BOTTOM
            )

            mapView.overlays.add(marker)
        }

        destination?.let {

            val point =
                GeoPoint(
                    it.latitude,
                    it.longitude
                )

            val marker =
                Marker(mapView)

            marker.position = point
            marker.title = it.displayName

            marker.setAnchor(
                Marker.ANCHOR_CENTER,
                Marker.ANCHOR_BOTTOM
            )

            mapView.overlays.add(marker)
        }

        routes.forEachIndexed { index, route ->

            val line =
                Polyline(mapView)

            line.setPoints(
                route.geometry
            )

            line.outlinePaint.color =
                if (index == selectedRoute) {
                    android.graphics.Color.GREEN
                } else {
                    android.graphics.Color.GRAY
                }

            line.outlinePaint.strokeWidth =
                if (index == selectedRoute) {
                    10f
                } else {
                    6f
                }

            mapView.overlays.add(line)
        }

        if (routes.isNotEmpty() && !isNavigating) {

            val selectedGeometry =
                routes
                    .getOrNull(selectedRoute)
                    ?.geometry
                    ?: emptyList()

            val points =
                if (selectedGeometry.isNotEmpty()) {
                    selectedGeometry
                } else {
                    routes.flatMap { it.geometry }
                }

            if (points.isNotEmpty()) {

                val bounds =
                    BoundingBox.fromGeoPoints(
                        points
                    )

                mapView.zoomToBoundingBox(
                    bounds,
                    true,
                    100
                )
            }
        }

        mapView.invalidate()
    }

    AndroidView(
        factory = {
            mapView
        },
        modifier =
            Modifier.fillMaxSize()
    )
}

// ============================================================
