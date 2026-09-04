package com.saferoute.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.saferoute.ai.traffic.TomTomTrafficService
import kotlinx.coroutines.launch

class TomTomTestActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            var status by remember {
                mutableStateOf(
                    "Press the button to test TomTom."
                )
            }

            var testing by remember {
                mutableStateOf(false)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(24.dp),
                verticalArrangement =
                    Arrangement.spacedBy(16.dp)
            ) {

                Text(
                    text = "SafeRoute AI",
                    style =
                        MaterialTheme.typography.headlineMedium
                )

                Text(
                    text = "TomTom Traffic API Test",
                    style =
                        MaterialTheme.typography.titleLarge
                )

                Text(
                    text =
                        "This test checks whether SafeRoute AI " +
                                "can communicate with TomTom Traffic " +
                                "using your API key."
                )

                Button(
                    modifier =
                        Modifier.fillMaxWidth(),
                    enabled = !testing,
                    onClick = {

                        testing = true

                        status =
                            "Connecting to TomTom..."

                        lifecycleScope.launch {

                            try {

                                /*
                                 * Mumbai test area.
                                 *
                                 * minLon = 72.80
                                 * minLat = 18.95
                                 * maxLon = 72.90
                                 * maxLat = 19.10
                                 */

                                val incidents =
                                    TomTomTrafficService
                                        .getIncidents(
                                            minLongitude = 72.80,
                                            minLatitude = 18.95,
                                            maxLongitude = 72.90,
                                            maxLatitude = 19.10
                                        )

                                status =
                                    buildString {

                                        append(
                                            "SUCCESS\n\n"
                                        )

                                        append(
                                            "TomTom API connection works.\n\n"
                                        )

                                        append(
                                            "Incidents returned: "
                                        )

                                        append(
                                            incidents.size
                                        )

                                        append("\n\n")

                                        if (
                                            incidents.isEmpty()
                                        ) {

                                            append(
                                                "No active traffic " +
                                                        "incidents were returned " +
                                                        "for this test area."
                                            )

                                        } else {

                                            incidents
                                                .take(10)
                                                .forEachIndexed {
                                                        index,
                                                        incident ->

                                                    append(
                                                        "${index + 1}. "
                                                    )

                                                    append(
                                                        incident.category
                                                    )

                                                    append(
                                                        " | "
                                                    )

                                                    append(
                                                        incident.magnitudeOfDelay
                                                    )

                                                    append("\n")

                                                    if (
                                                        incident.description
                                                            .isNotBlank()
                                                    ) {
                                                        append(
                                                            incident.description
                                                        )

                                                        append("\n")
                                                    }

                                                    if (
                                                        incident.roadNumbers
                                                            .isNotEmpty()
                                                    ) {
                                                        append(
                                                            "Road: "
                                                        )

                                                        append(
                                                            incident
                                                                .roadNumbers
                                                                .joinToString(
                                                                    ", "
                                                                )
                                                        )

                                                        append("\n")
                                                    }

                                                    if (
                                                        incident.delayInSeconds
                                                        != null
                                                    ) {
                                                        append(
                                                            "Delay: "
                                                        )

                                                        append(
                                                            incident
                                                                .delayInSeconds
                                                        )

                                                        append(
                                                            " seconds\n"
                                                        )
                                                    }

                                                    append("\n")
                                                }
                                        }
                                    }

                            } catch (exception: Exception) {

                                status =
                                    buildString {

                                        append(
                                            "FAILED\n\n"
                                        )

                                        append(
                                            exception.message
                                                ?: exception
                                                    .javaClass
                                                    .simpleName
                                        )
                                    }

                            } finally {

                                testing = false
                            }
                        }
                    }
                ) {
                    Text(
                        text =
                            if (testing) {
                                "Testing..."
                            } else {
                                "Test TomTom"
                            }
                    )
                }

                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        modifier =
                            Modifier.padding(16.dp),
                        text = status
                    )
                }
            }
        }
    }
}