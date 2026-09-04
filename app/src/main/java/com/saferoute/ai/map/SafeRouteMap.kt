package com.saferoute.ai.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import com.saferoute.ai.model.RouteOption
import com.saferoute.ai.model.SearchResult

@Composable
fun SafeRouteMap(
    currentLocation: GeoPoint?,
    destination: SearchResult?,
    routes: List<RouteOption>,
    selectedRoute: Int,
    isNavigating: Boolean = false,
    recenterRequest: Int = 0
) {
    val context = LocalContext.current

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            setBuiltInZoomControls(false)
            setMultiTouchControls(true)
        }
    }

    LaunchedEffect(currentLocation, destination, isNavigating) {
        if (!isNavigating && currentLocation != null && destination == null) {
            mapView.controller.animateTo(currentLocation)
        }
    }

    LaunchedEffect(isNavigating) {
        if (isNavigating && currentLocation != null) {
            mapView.controller.setZoom(17.0)
            mapView.controller.animateTo(currentLocation)
        }
    }

    LaunchedEffect(currentLocation, isNavigating) {
        if (isNavigating && currentLocation != null) {
            mapView.controller.animateTo(currentLocation)
        }
    }

    LaunchedEffect(recenterRequest) {
        if (recenterRequest > 0 && currentLocation != null) {
            mapView.controller.setZoom(17.0)
            mapView.controller.animateTo(currentLocation)
        }
    }

    LaunchedEffect(currentLocation, destination, routes, selectedRoute, isNavigating) {
        mapView.overlays.clear()

        currentLocation?.let { location ->
            val marker = Marker(mapView)
            marker.position = location
            marker.title = "You are here"
            marker.icon = BitmapDrawable(context.resources, createCarBitmap())
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            mapView.overlays.add(marker)
        }

        destination?.let { destinationResult ->
            val point = GeoPoint(destinationResult.latitude, destinationResult.longitude)
            val marker = Marker(mapView)
            marker.position = point
            marker.title = destinationResult.displayName
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.overlays.add(marker)
        }

        routes.forEachIndexed { index, route ->
            if (isNavigating && index != selectedRoute) return@forEachIndexed
            if (route.geometry.isEmpty()) return@forEachIndexed

            val isSelected = index == selectedRoute

            val casing = Polyline(mapView)
            casing.setPoints(route.geometry)
            casing.outlinePaint.color = if (isSelected) {
                android.graphics.Color.WHITE
            } else {
                android.graphics.Color.TRANSPARENT
            }
            casing.outlinePaint.strokeWidth = if (isSelected) {
                if (isNavigating) 17f else 12f
            } else {
                0f
            }
            casing.outlinePaint.isAntiAlias = true
            mapView.overlays.add(casing)

            val line = Polyline(mapView)
            line.setPoints(route.geometry)
            line.outlinePaint.color = if (isSelected) {
                android.graphics.Color.rgb(66, 133, 244)
            } else {
                android.graphics.Color.rgb(145, 150, 158)
            }
            line.outlinePaint.strokeWidth = if (isSelected) {
                if (isNavigating) 11f else 8f
            } else {
                5f
            }
            line.outlinePaint.isAntiAlias = true
            mapView.overlays.add(line)
        }

        if (routes.isNotEmpty() && !isNavigating) {
            val selectedGeometry = routes.getOrNull(selectedRoute)?.geometry ?: emptyList()
            val points = if (selectedGeometry.isNotEmpty()) {
                selectedGeometry
            } else {
                routes.flatMap { it.geometry }
            }

            if (points.isNotEmpty()) {
                val bounds = BoundingBox.fromGeoPoints(points)
                mapView.zoomToBoundingBox(bounds, true, 100)
            }
        }

        mapView.invalidate()
    }

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize()
    )
}

private fun createCarBitmap(): Bitmap {
    val size = 96
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(110, 0, 0, 0)
    }
    canvas.drawCircle(48f, 53f, 31f, shadow)

    val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(220, 45, 55)
    }
    val path = Path().apply {
        moveTo(25f, 67f)
        lineTo(22f, 42f)
        quadTo(23f, 34f, 31f, 31f)
        lineTo(38f, 22f)
        quadTo(48f, 17f, 58f, 22f)
        lineTo(65f, 31f)
        quadTo(73f, 34f, 74f, 42f)
        lineTo(71f, 67f)
        quadTo(68f, 75f, 60f, 78f)
        lineTo(36f, 78f)
        quadTo(28f, 75f, 25f, 67f)
        close()
    }
    canvas.drawPath(path, body)

    val glass = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(105, 190, 205)
    }
    val glassPath = Path().apply {
        moveTo(33f, 34f)
        lineTo(39f, 26f)
        quadTo(48f, 22f, 57f, 26f)
        lineTo(63f, 34f)
        close()
    }
    canvas.drawPath(glassPath, glass)

    val wheel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
    }
    canvas.drawCircle(28f, 64f, 6f, wheel)
    canvas.drawCircle(68f, 64f, 6f, wheel)

    return bitmap
}
