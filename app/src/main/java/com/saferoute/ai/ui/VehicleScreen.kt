package com.saferoute.ai.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saferoute.ai.core.Background
import com.saferoute.ai.core.Border
import com.saferoute.ai.core.Green
import com.saferoute.ai.core.GreenDark
import com.saferoute.ai.core.SurfaceColor
import com.saferoute.ai.core.SurfaceLight
import com.saferoute.ai.core.TextPrimary
import com.saferoute.ai.core.TextSecondary
import com.saferoute.ai.core.TextTertiary
import com.saferoute.ai.model.VehicleProfile

// ============================================================
// VEHICLE SCREEN
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleScreen(
    vehicle: VehicleProfile,
    onSave: (VehicleProfile) -> Unit,
    onBack: () -> Unit
) {

    var vehicleType by remember(vehicle) {
        mutableStateOf(vehicle.vehicleType)
    }

    var make by remember(vehicle) {
        mutableStateOf(vehicle.make)
    }

    var model by remember(vehicle) {
        mutableStateOf(vehicle.model)
    }

    var year by remember(vehicle) {
        mutableStateOf(vehicle.year)
    }

    var fuel by remember(vehicle) {
        mutableStateOf(vehicle.fuel)
    }

    val vehicleTypes = listOf(
        "Car",
        "SUV",
        "Hatchback",
        "Sedan",
        "MPV",
        "Pickup Truck",
        "Motorcycle"
    )

    val makes = listOf(
        "Audi",
        "BMW",
        "Honda",
        "Hyundai",
        "Jeep",
        "Kia",
        "Mahindra",
        "Maruti Suzuki",
        "Mercedes-Benz",
        "MG",
        "Nissan",
        "Renault",
        "Skoda",
        "Tata",
        "Toyota",
        "Volkswagen"
    )

    val modelsByMake = mapOf(

        "Honda" to listOf(
            "Amaze",
            "City",
            "City e:HEV",
            "Elevate",
            "ZR-V"
        ),

        "Maruti Suzuki" to listOf(
            "Alto K10",
            "S-Presso",
            "Celerio",
            "Wagon R",
            "Eeco",
            "Swift",
            "Dzire",
            "Brezza",
            "Ertiga",
            "Baleno",
            "Fronx",
            "Grand Vitara",
            "Jimny",
            "XL6",
            "Invicto",
            "e Vitara"
        ),

        "Hyundai" to listOf(
            "Grand i10 Nios",
            "i20",
            "Aura",
            "Exter",
            "Venue",
            "Verna",
            "Creta",
            "Alcazar",
            "Tucson",
            "Ioniq 5"
        ),

        "Tata" to listOf(
            "Tiago",
            "Tigor",
            "Altroz",
            "Punch",
            "Nexon",
            "Curvv",
            "Harrier",
            "Safari",
            "Nexon EV",
            "Punch EV",
            "Tiago EV"
        ),

        "Toyota" to listOf(
            "Glanza",
            "Urban Cruiser Hyryder",
            "Innova Crysta",
            "Innova HyCross",
            "Fortuner",
            "Camry",
            "Vellfire"
        ),

        "Kia" to listOf(
            "Sonet",
            "Seltos",
            "Carens",
            "Carnival"
        ),

        "Mahindra" to listOf(
            "Bolero",
            "Scorpio",
            "Scorpio N",
            "Thar",
            "XUV 3XO",
            "XUV700",
            "Marazzo"
        ),

        "MG" to listOf(
            "Comet EV",
            "Astor",
            "Hector",
            "Gloster",
            "ZS EV",
            "Windsor EV"
        ),

        "Volkswagen" to listOf(
            "Polo",
            "Virtus",
            "Taigun",
            "Tiguan"
        ),

        "Skoda" to listOf(
            "Slavia",
            "Kushaq",
            "Kodiaq",
            "Superb"
        ),

        "Renault" to listOf(
            "Kwid",
            "Triber",
            "Kiger",
            "Duster"
        ),

        "Nissan" to listOf(
            "Magnite",
            "X-Trail"
        ),

        "Jeep" to listOf(
            "Compass",
            "Meridian",
            "Wrangler",
            "Grand Cherokee"
        ),

        "BMW" to listOf(
            "2 Series",
            "3 Series",
            "5 Series",
            "7 Series",
            "X1",
            "X3",
            "X5",
            "X7"
        ),

        "Mercedes-Benz" to listOf(
            "A-Class",
            "C-Class",
            "E-Class",
            "S-Class",
            "GLA",
            "GLC",
            "GLE",
            "GLS"
        ),

        "Audi" to listOf(
            "A4",
            "A6",
            "A8",
            "Q3",
            "Q5",
            "Q7",
            "Q8"
        )
    )

    val availableModels =
        modelsByMake[make] ?: emptyList()

    val years = (2026 downTo 1990).map {
        it.toString()
    }

    val fuelOptions = when (model) {

        "City e:HEV" ->
            listOf("Hybrid")

        "Nexon EV",
        "Punch EV",
        "Tiago EV",
        "Ioniq 5",
        "Comet EV",
        "ZS EV",
        "Windsor EV",
        "e Vitara" ->
            listOf("Electric")

        else ->
            listOf(
                "Petrol",
                "Diesel",
                "CNG",
                "Hybrid",
                "Electric"
            )
    }

    val canSave =
        make.isNotBlank() &&
                model.isNotBlank() &&
                year.isNotBlank() &&
                fuel.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {

        // ====================================================
        // TOP BAR
        // ====================================================

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Column {

                Text(
                    text = "Vehicle",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Tell SafeRoute about your vehicle",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        // ====================================================
        // CONTENT
        // ====================================================

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 20.dp
                )
                .padding(
                    bottom = 24.dp
                ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // =================================================
            // VEHICLE ICON HEADER
            // =================================================

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(18.dp)
                    )
                    .background(GreenDark)
                    .padding(20.dp)
            ) {

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(
                                RoundedCornerShape(15.dp)
                            )
                            .background(Green),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(14.dp)
                    )

                    Column {

                        Text(
                            text = "Vehicle profile",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Used to personalize route recommendations",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // =================================================
            // VEHICLE TYPE
            // =================================================

            DropdownField(
                label = "Vehicle type",
                value = vehicleType,
                options = vehicleTypes,
                onSelected = {
                    vehicleType = it
                }
            )

            // =================================================
            // MAKE
            // =================================================

            DropdownField(
                label = "Manufacturer",
                value = make,
                placeholder = "Select manufacturer",
                options = makes,
                onSelected = {

                    make = it

                    // Reset dependent fields
                    model = ""
                    fuel = ""
                }
            )

            // =================================================
            // MODEL
            // =================================================

            DropdownField(
                label = "Model",
                value = model,
                placeholder =
                    if (make.isBlank()) {
                        "Select manufacturer first"
                    } else {
                        "Select model"
                    },
                options = availableModels,
                enabled = make.isNotBlank(),
                onSelected = {

                    model = it

                    // Automatically select the
                    // first valid fuel option.
                    val fuels = when (it) {

                        "City e:HEV" ->
                            listOf("Hybrid")

                        "Nexon EV",
                        "Punch EV",
                        "Tiago EV",
                        "Ioniq 5",
                        "Comet EV",
                        "ZS EV",
                        "Windsor EV",
                        "e Vitara" ->
                            listOf("Electric")

                        else ->
                            listOf(
                                "Petrol",
                                "Diesel",
                                "CNG",
                                "Hybrid",
                                "Electric"
                            )
                    }

                    fuel = fuels.first()
                }
            )

            // =================================================
            // YEAR
            // =================================================

            DropdownField(
                label = "Manufacturing year",
                value = year,
                placeholder = "Select year",
                options = years,
                onSelected = {
                    year = it
                }
            )

            // =================================================
            // FUEL
            // =================================================

            DropdownField(
                label = "Fuel type",
                value = fuel,
                placeholder =
                    if (model.isBlank()) {
                        "Select model first"
                    } else {
                        "Select fuel type"
                    },
                options = fuelOptions,
                enabled = model.isNotBlank(),
                onSelected = {
                    fuel = it
                }
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            // =================================================
            // SAVE BUTTON
            // =================================================

            Button(
                onClick = {

                    if (canSave) {

                        onSave(
                            VehicleProfile(
                                vehicleType = vehicleType,
                                make = make,
                                model = model,
                                year = year,
                                fuel = fuel
                            )
                        )
                    }
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Green,
                    contentColor = Color.Black,
                    disabledContainerColor =
                        SurfaceLight,
                    disabledContentColor =
                        TextTertiary
                )
            ) {

                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Text(
                    text = "Save vehicle",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // =================================================
            // INFO
            // =================================================

            Text(
                text =
                    "SafeRoute will use your vehicle profile when calculating future safety recommendations.",
                color = TextTertiary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(
                    horizontal = 4.dp
                )
            )
        }
    }
}

// ============================================================
// DROPDOWN FIELD
// ============================================================

@Composable
private fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    placeholder: String = "Select",
    enabled: Boolean = true
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        Text(
            text = label,
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(
                bottom = 7.dp,
                start = 2.dp
            )
        )

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(
                        RoundedCornerShape(14.dp)
                    )
                    .background(
                        if (enabled) {
                            SurfaceColor
                        } else {
                            SurfaceLight
                        }
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            Border
                        ),
                        RoundedCornerShape(14.dp)
                    )
                    .clickable(
                        enabled = enabled &&
                                options.isNotEmpty()
                    ) {
                        expanded = true
                    }
                    .padding(
                        horizontal = 16.dp
                    ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text =
                        if (value.isBlank()) {
                            placeholder
                        } else {
                            value
                        },
                    color =
                        if (value.isBlank()) {
                            TextTertiary
                        } else {
                            TextPrimary
                        },
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector =
                        Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (enabled) {
                        TextSecondary
                    } else {
                        TextTertiary
                    }
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                },
                modifier = Modifier
                    .background(SurfaceColor)
            ) {

                options.forEach { option ->

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                color = TextPrimary
                            )
                        },
                        onClick = {

                            onSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}