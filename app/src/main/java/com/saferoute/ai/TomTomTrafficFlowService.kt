package com.saferoute.ai.traffic

import com.saferoute.ai.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.*

data class TrafficFlowResult(
    val matchedSegments: Int,
    val averageRelativeSpeed: Double?,
    val minimumRelativeSpeed: Double?,
    val averageCurrentSpeedKph: Double?,
    val closedSegments: Int,
    val trafficLevel: String,
    val estimatedDelayFraction: Double
)

private data class FlowFeature(
    val points: List<Pair<Double, Double>>,
    val relativeSpeed: Double?,
    val absoluteSpeed: Double?,
    val roadClosure: Boolean
)

object TomTomTrafficFlowService {
    private const val BASE_URL =
        "https://api.tomtom.com/maps/orbis/traffic/tile/flow"
    private const val ZOOM = 15
    private const val EXTENT = 4096.0

    suspend fun analyzeRoute(
        route: List<GeoPoint>
    ): TrafficFlowResult = withContext(Dispatchers.IO) {
        if (route.size < 2) {
            return@withContext TrafficFlowResult(0, null, null, null, 0, "UNKNOWN", 0.0)
        }

        val key = BuildConfig.TOMTOM_API_KEY.trim()
        require(key.isNotEmpty()) { "TomTom API key is missing." }

        // Only download tiles actually crossed by the route.
        // The previous 3x3-neighbour expansion could turn a normal route
        // into dozens/hundreds of sequential HTTP requests.
        val tiles = route
            .asSequence()
            .map { tileFor(it.latitude, it.longitude, ZOOM) }
            .toSet()

        val features = coroutineScope {
            tiles.map { (x, y) ->
                async(Dispatchers.IO) {
                    runCatching {
                        val bytes = downloadTile(x, y, key)
                        if (bytes.isNotEmpty()) {
                            decodeTile(bytes, x, y, ZOOM)
                        } else {
                            emptyList()
                        }
                    }.getOrDefault(emptyList())
                }
            }.awaitAll().flatten().toMutableList()
        }

        // OSRM geometry can contain hundreds/thousands of points.
        // Sample it for matching so traffic analysis stays fast.
        val routeSamples = route
            .asSequence()
            .filterIndexed { index, _ -> index % 5 == 0 }
            .toList()

        val matched = mutableListOf<FlowFeature>()
        for (feature in features) {
            if (feature.points.size < 2) continue

            val near = feature.points.any { (lat, lon) ->
                routeSamples.any { p ->
                    distanceMeters(
                        p.latitude,
                        p.longitude,
                        lat,
                        lon
                    ) <= 45.0
                }
            }

            if (near) matched += feature
        }

        if (matched.isEmpty()) {
            return@withContext TrafficFlowResult(
                0, null, null, null, 0, "UNKNOWN", 0.0
            )
        }

        val speeds = matched.mapNotNull { it.relativeSpeed }.filter { it in 0.0..1.0 }
        val absolute = matched.mapNotNull { it.absoluteSpeed }.filter { it >= 0.0 }
        val closed = matched.count { it.roadClosure }

        val avg = speeds.average().takeIf { speeds.isNotEmpty() }
        val min = speeds.minOrNull()
        val avgAbs = absolute.average().takeIf { absolute.isNotEmpty() }

        // OSRM gives the baseline route duration. Relative speed lets us estimate
        // how much slower the currently observed traffic is than free flow.
        val relative = avg ?: 1.0
        val delayFraction = if (relative > 0.05) {
            (1.0 / relative - 1.0).coerceIn(0.0, 3.0)
        } else {
            3.0
        }

        val level = when {
            closed > 0 -> "CLOSED"
            relative < 0.30 -> "HEAVY"
            relative < 0.55 -> "MODERATE"
            relative < 0.80 -> "LIGHT"
            else -> "FREE"
        }

        TrafficFlowResult(
            matchedSegments = matched.size,
            averageRelativeSpeed = avg,
            minimumRelativeSpeed = min,
            averageCurrentSpeedKph = avgAbs,
            closedSegments = closed,
            trafficLevel = level,
            estimatedDelayFraction = delayFraction
        )
    }

    private fun downloadTile(x: Int, y: Int, key: String): ByteArray {
        val url = URL(
            "$BASE_URL/$ZOOM/$x/$y.pbf?apiVersion=1" +
                    "&key=$key" +
                    "&tags=road_category,relative_speed,absolute_speed,road_closure"
        )
        val c = url.openConnection() as HttpURLConnection
        return try {
            c.requestMethod = "GET"
            c.connectTimeout = 5_000
            c.readTimeout = 8_000
            c.setRequestProperty("TomTom-Api-Version", "1")
            c.setRequestProperty("Accept", "application/x-protobuf")
            if (c.responseCode !in 200..299) return ByteArray(0)
            c.inputStream.use { it.readBytes() }
        } finally {
            c.disconnect()
        }
    }

    private fun tileFor(lat: Double, lon: Double, zoom: Int): Pair<Int, Int> {
        val n = 2.0.pow(zoom)
        val x = floor((lon + 180.0) / 360.0 * n).toInt()
        val latRad = Math.toRadians(lat.coerceIn(-85.05112878, 85.05112878))
        val y = floor(
            (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / Math.PI) / 2.0 * n
        ).toInt()
        return x to y
    }

    private fun decodeTile(
        bytes: ByteArray,
        tileX: Int,
        tileY: Int,
        zoom: Int
    ): List<FlowFeature> {
        val tile = ProtoReader(bytes)
        val result = mutableListOf<FlowFeature>()

        while (!tile.eof()) {
            val tag = tile.readTag()
            if (tag.fieldNumber == 0) break
            if (tag.fieldNumber == 3 && tag.wireType == 2) {
                val layer = ProtoReader(tile.readBytes())
                var name = ""
                var extent = EXTENT.toInt()
                val keys = mutableListOf<String>()
                val values = mutableListOf<Any?>()
                val featureBytes = mutableListOf<ByteArray>()

                while (!layer.eof()) {
                    val lt = layer.readTag()
                    when (lt.fieldNumber) {
                        1 -> if (lt.wireType == 2) name = layer.readString()
                        2 -> if (lt.wireType == 2) featureBytes += layer.readBytes()
                        3 -> if (lt.wireType == 2) keys += layer.readString()
                        4 -> if (lt.wireType == 2) values += decodeValue(layer.readBytes())
                        5 -> if (lt.wireType == 0) extent = layer.readVarInt().toInt()
                        else -> layer.skip(lt.wireType)
                    }
                }

                if (name.equals("Traffic flow", true)) {
                    featureBytes.forEach { fb ->
                        decodeFeature(
                            fb, keys, values, extent,
                            tileX, tileY, zoom
                        )?.let(result::add)
                    }
                }
            } else {
                tile.skip(tag.wireType)
            }
        }
        return result
    }

    private fun decodeFeature(
        bytes: ByteArray,
        keys: List<String>,
        values: List<Any?>,
        extent: Int,
        tileX: Int,
        tileY: Int,
        zoom: Int
    ): FlowFeature? {
        val r = ProtoReader(bytes)
        val tags = mutableListOf<Long>()
        val geometry = mutableListOf<Long>()
        var type = 0

        while (!r.eof()) {
            val t = r.readTag()
            when (t.fieldNumber) {
                2 -> if (t.wireType == 2) {
                    val packed = ProtoReader(r.readBytes())
                    while (!packed.eof()) tags += packed.readVarInt()
                } else if (t.wireType == 0) tags += r.readVarInt()
                3 -> if (t.wireType == 0) type = r.readVarInt().toInt()
                4 -> if (t.wireType == 2) {
                    val packed = ProtoReader(r.readBytes())
                    while (!packed.eof()) geometry += packed.readVarInt()
                } else if (t.wireType == 0) geometry += r.readVarInt()
                else -> r.skip(t.wireType)
            }
        }

        if (type != 2 || geometry.isEmpty()) return null

        val props = mutableMapOf<String, Any?>()
        var i = 0
        while (i + 1 < tags.size) {
            val k = keys.getOrNull(tags[i].toInt())
            val v = values.getOrNull(tags[i + 1].toInt())
            if (k != null) props[k] = v
            i += 2
        }

        val points = decodeGeometry(geometry, extent, tileX, tileY, zoom)
        if (points.size < 2) return null

        return FlowFeature(
            points = points,
            relativeSpeed = (props["relative_speed"] as? Number)?.toDouble(),
            absoluteSpeed = (props["absolute_speed"] as? Number)?.toDouble(),
            roadClosure = props["road_closure"] == true
        )
    }

    private fun decodeGeometry(
        geometry: List<Long>,
        extent: Int,
        tileX: Int,
        tileY: Int,
        zoom: Int
    ): List<Pair<Double, Double>> {
        val out = mutableListOf<Pair<Double, Double>>()
        var cursorX = 0L
        var cursorY = 0L
        var index = 0

        while (index < geometry.size) {
            val commandAndCount = geometry[index++]
            val command = (commandAndCount and 7L).toInt()
            val count = (commandAndCount ushr 3).toInt()

            when (command) {
                1, 2 -> {
                    repeat(count) {
                        if (index + 1 >= geometry.size) return@repeat
                        cursorX += zigzagDecode(geometry[index++])
                        cursorY += zigzagDecode(geometry[index++])

                        val n = 2.0.pow(zoom)
                        val worldX = tileX + cursorX.toDouble() / extent
                        val worldY = tileY + cursorY.toDouble() / extent
                        val lon = worldX / n * 360.0 - 180.0
                        val y2 = Math.PI * (1.0 - 2.0 * worldY / n)
                        val lat = Math.toDegrees(atan(sinh(y2)))
                        out += lat to lon
                    }
                }
                7 -> Unit
                else -> break
            }
        }
        return out
    }

    private fun zigzagDecode(v: Long): Long =
        (v ushr 1) xor -(v and 1L)

    private fun decodeValue(bytes: ByteArray): Any? {
        val r = ProtoReader(bytes)
        var result: Any? = null
        while (!r.eof()) {
            val t = r.readTag()
            when (t.fieldNumber) {
                1 -> if (t.wireType == 2) result = r.readString()
                2 -> if (t.wireType == 5) result = r.readFloat()
                3 -> if (t.wireType == 1) result = r.readDouble()
                4, 5, 6 -> if (t.wireType == 0) result = r.readVarInt()
                7 -> if (t.wireType == 0) result = r.readVarInt() != 0L
                else -> r.skip(t.wireType)
            }
        }
        return result
    }

    private fun distanceMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) *
                cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        return r * 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
    }

    private class ProtoReader(private val bytes: ByteArray) {
        private var pos = 0

        fun eof() = pos >= bytes.size

        fun readTag(): Tag {
            val v = readVarInt()
            return Tag((v ushr 3).toInt(), (v and 7L).toInt())
        }

        fun readVarInt(): Long {
            var result = 0L
            var shift = 0
            while (pos < bytes.size && shift < 64) {
                val b = bytes[pos++].toInt() and 0xff
                result = result or ((b and 0x7f).toLong() shl shift)
                if ((b and 0x80) == 0) return result
                shift += 7
            }
            return result
        }

        fun readBytes(): ByteArray {
            val length = readVarInt().toInt()
            if (length < 0 || pos + length > bytes.size) return ByteArray(0)
            return bytes.copyOfRange(pos, pos + length).also {
                pos += length
            }
        }

        fun readString(): String = readBytes().toString(Charsets.UTF_8)

        fun readFloat(): Float {
            val v = readFixed32()
            return Float.fromBits(v)
        }

        fun readDouble(): Double {
            var v = 0L
            repeat(8) { v = v or ((bytes[pos++].toLong() and 0xffL) shl (8 * it)) }
            return Double.fromBits(v)
        }

        private fun readFixed32(): Int {
            var v = 0
            repeat(4) { v = v or ((bytes[pos++].toInt() and 0xff) shl (8 * it)) }
            return v
        }

        fun skip(wireType: Int) {
            when (wireType) {
                0 -> readVarInt()
                1 -> pos = (pos + 8).coerceAtMost(bytes.size)
                2 -> {
                    val n = readVarInt().toInt()
                    pos = (pos + n).coerceAtMost(bytes.size)
                }
                5 -> pos = (pos + 4).coerceAtMost(bytes.size)
                else -> pos = bytes.size
            }
        }
    }

    private data class Tag(val fieldNumber: Int, val wireType: Int)
}
