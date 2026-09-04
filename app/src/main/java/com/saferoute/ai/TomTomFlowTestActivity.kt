package com.saferoute.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saferoute.ai.traffic.TomTomTrafficFlowService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

class TomTomFlowTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var resultText by remember { mutableStateOf("Press TEST TRAFFIC FLOW") }
            var loading by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            MaterialTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "TOMTOM TRAFFIC FLOW",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Button(
                        enabled = !loading,
                        onClick = {
                            loading = true
                            resultText = "Downloading traffic tiles..."
                            scope.launch(Dispatchers.Main) {
                                try {
                                    val result =
                                        TomTomTrafficFlowService.analyzeRoute(
                                            listOf(
                                                GeoPoint(19.0330, 73.0297),
                                                GeoPoint(19.0473, 73.0705),
                                                GeoPoint(19.0540, 73.0900)
                                            )
                                        )

                                    resultText = buildString {
                                        appendLine("SUCCESS")
                                        appendLine()
                                        appendLine("Matched segments: ${result.matchedSegments}")
                                        appendLine(
                                            "Average relative speed: " +
                                                (result.averageRelativeSpeed?.let {
                                                    "%.2f".format(it)
                                                } ?: "--")
                                        )
                                        appendLine(
                                            "Minimum relative speed: " +
                                                (result.minimumRelativeSpeed?.let {
                                                    "%.2f".format(it)
                                                } ?: "--")
                                        )
                                        appendLine(
                                            "Average current speed: " +
                                                (result.averageCurrentSpeedKph?.let {
                                                    "%.1f km/h".format(it)
                                                } ?: "--")
                                        )
                                        appendLine("Closed segments: ${result.closedSegments}")
                                        appendLine("Traffic: ${result.trafficLevel}")
                                        appendLine(
                                            "Estimated delay: " +
                                                "%.0f%%".format(
                                                    result.estimatedDelayFraction * 100.0
                                                )
                                        )
                                    }
                                } catch (e: Exception) {
                                    resultText =
                                        "FAILED\n\n${e.message ?: e.javaClass.simpleName}"
                                } finally {
                                    loading = false
                                }
                            }
                        }
                    ) {
                        Text("TEST TRAFFIC FLOW")
                    }

                    if (loading) {
                        CircularProgressIndicator()
                    }

                    Text(resultText)
                }
            }
        }
    }
}
