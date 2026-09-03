package com.saferoute.ai.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saferoute.ai.core.*
import com.saferoute.ai.model.VehicleProfile
import org.osmdroid.util.GeoPoint

// ============================================================
// HOME SCREEN
// ============================================================

@Composable
fun HomeScreen(
    currentLocation: GeoPoint?,
    vehicle: VehicleProfile,
    onSearchDestination: () -> Unit,
    onVehicle: () -> Unit,
    onProfile: () -> Unit,
    onExplore: () -> Unit
) {
    Scaffold(
        containerColor = Background,

        bottomBar = {
            CleanBottomBar(
                selected = "Home",
                onSelected = { tab ->

                    when (tab) {
                        "Home" -> {}
                        "Explore" -> onExplore()
                        "Vehicle" -> onVehicle()
                        "Profile" -> onProfile()
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Background)
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp)
        ) {

            Spacer(
                modifier =
                    Modifier.height(24.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column {

                    Text(
                        text = "SafeRoute",
                        color = TextPrimary,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Travel safer.",
                        color = TextSecondary,
                        fontSize = 15.sp
                    )
                }

                Box(
                    modifier =
                        Modifier
                            .size(42.dp)
                            .clip(
                                RoundedCornerShape(14.dp)
                            )
                            .background(
                                SurfaceColor
                            )
                            .border(
                                1.dp,
                                Border,
                                RoundedCornerShape(14.dp)
                            ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Settings,

                        contentDescription =
                            "Settings",

                        tint =
                            TextSecondary
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(32.dp)
            )

            Text(
                text =
                    "Where do you want to go?",

                color =
                    TextPrimary,

                fontSize =
                    22.sp,

                fontWeight =
                    FontWeight.SemiBold
            )

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            DestinationCard(
                icon =
                    Icons.Default.Search,

                title =
                    "Search destination",

                subtitle =
                    if (currentLocation != null) {
                        "Your live location is ready"
                    } else {
                        "Waiting for GPS location"
                    },

                onClick =
                    onSearchDestination
            )

            Spacer(
                modifier =
                    Modifier.height(28.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text =
                        "Vehicle",

                    color =
                        TextPrimary,

                    fontSize =
                        17.sp,

                    fontWeight =
                        FontWeight.SemiBold
                )

                Text(
                    text =
                        if (vehicle.isSaved()) {
                            "Saved"
                        } else {
                            "Set up"
                        },

                    color =
                        Green,

                    fontSize =
                        14.sp
                )
            }

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            VehicleCard(
                vehicle =
                    vehicle,

                onClick =
                    onVehicle
            )

            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )

            SafetyInfoCard()
        }
    }
}

// ============================================================
// DESTINATION CARD
// ============================================================

@Composable
fun DestinationCard(
    icon:
    androidx.compose.ui.graphics.vector.ImageVector,

    title: String,
    subtitle: String,
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
            RoundedCornerShape(18.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    SurfaceColor
            ),

        border =
            BorderStroke(
                1.dp,
                Border
            )
    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(17.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(

                modifier =
                    Modifier
                        .size(42.dp)
                        .clip(
                            RoundedCornerShape(13.dp)
                        )
                        .background(
                            GreenDark
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        icon,

                    contentDescription =
                        null,

                    tint =
                        Green
                )
            }

            Spacer(
                modifier =
                    Modifier.width(14.dp)
            )

            Column {

                Text(
                    text =
                        title,

                    color =
                        TextPrimary,

                    fontSize =
                        15.sp,

                    fontWeight =
                        FontWeight.SemiBold
                )

                Spacer(
                    modifier =
                        Modifier.height(3.dp)
                )

                Text(
                    text =
                        subtitle,

                    color =
                        TextSecondary,

                    fontSize =
                        12.sp,

                    maxLines =
                        1,

                    overflow =
                        TextOverflow.Ellipsis
                )
            }

            Spacer(
                modifier =
                    Modifier.width(8.dp)
            )

            Icon(
                imageVector =
                    Icons.Default.ArrowForward,

                contentDescription =
                    null,

                tint =
                    TextTertiary
            )
        }
    }
}

// ============================================================
// VEHICLE CARD
// ============================================================

@Composable
fun VehicleCard(
    vehicle: VehicleProfile,
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
            RoundedCornerShape(18.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    SurfaceColor
            ),

        border =
            BorderStroke(
                1.dp,
                Border
            )
    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(17.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(

                modifier =
                    Modifier
                        .size(42.dp)
                        .clip(
                            RoundedCornerShape(13.dp)
                        )
                        .background(
                            SurfaceLight
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Default.DirectionsCar,

                    contentDescription =
                        null,

                    tint =
                        Green
                )
            }

            Spacer(
                modifier =
                    Modifier.width(14.dp)
            )

            Column {

                Text(

                    text =
                        if (vehicle.isSaved()) {
                            vehicle.displayName()
                        } else {
                            "Add your vehicle"
                        },

                    color =
                        TextPrimary,

                    fontSize =
                        15.sp,

                    fontWeight =
                        FontWeight.SemiBold,

                    maxLines =
                        1,

                    overflow =
                        TextOverflow.Ellipsis
                )

                Spacer(
                    modifier =
                        Modifier.height(3.dp)
                )

                Text(

                    text =
                        if (vehicle.isSaved()) {
                            "${vehicle.fuel} • ${
                                vehicle.year.ifBlank {
                                    "Year not set"
                                }
                            }"
                        } else {
                            "Personalize route safety"
                        },

                    color =
                        TextSecondary,

                    fontSize =
                        12.sp
                )
            }

            Spacer(
                modifier =
                    Modifier.width(8.dp)
            )

            Icon(
                imageVector =
                    Icons.Default.ArrowForward,

                contentDescription =
                    null,

                tint =
                    TextTertiary
            )
        }
    }
}

// ============================================================
// SAFETY CARD
// ============================================================

@Composable
fun SafetyInfoCard() {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(18.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    GreenDark
            )
    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(17.dp),

            verticalAlignment =
                Alignment.Top
        ) {

            Icon(

                imageVector =
                    Icons.Default.Security,

                contentDescription =
                    null,

                tint =
                    Green,

                modifier =
                    Modifier.size(25.dp)
            )

            Spacer(
                modifier =
                    Modifier.width(13.dp)
            )

            Column {

                Text(

                    text =
                        "Safety-aware navigation",

                    color =
                        TextPrimary,

                    fontSize =
                        15.sp,

                    fontWeight =
                        FontWeight.SemiBold
                )

                Spacer(
                    modifier =
                        Modifier.height(5.dp)
                )

                Text(

                    text =
                        "Weather, roads, traffic and incidents can be considered when choosing safer routes.",

                    color =
                        TextSecondary,

                    fontSize =
                        12.sp,

                    lineHeight =
                        18.sp
                )
            }
        }
    }
}

// ============================================================
// BOTTOM BAR
// ============================================================

@Composable
fun CleanBottomBar(
    selected: String,
    onSelected: (String) -> Unit
) {

    Surface(
        color =
            Background
    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 9.dp
                    ),

            horizontalArrangement =
                Arrangement.SpaceEvenly
        ) {

            BottomItem(
                title =
                    "Home",

                icon =
                    Icons.Default.Home,

                selected =
                    selected == "Home",

                onClick = {
                    onSelected("Home")
                }
            )

            BottomItem(
                title =
                    "Explore",

                icon =
                    Icons.Default.Map,

                selected =
                    selected == "Explore",

                onClick = {
                    onSelected("Explore")
                }
            )

            BottomItem(
                title =
                    "Vehicle",

                icon =
                    Icons.Default.DirectionsCar,

                selected =
                    selected == "Vehicle",

                onClick = {
                    onSelected("Vehicle")
                }
            )

            BottomItem(
                title =
                    "Profile",

                icon =
                    Icons.Default.Person,

                selected =
                    selected == "Profile",

                onClick = {
                    onSelected("Profile")
                }
            )
        }
    }
}

// ============================================================
// BOTTOM ITEM
// ============================================================

@Composable
fun BottomItem(
    title: String,

    icon:
    androidx.compose.ui.graphics.vector.ImageVector,

    selected: Boolean,

    onClick: () -> Unit
) {

    Column(

        modifier =
            Modifier
                .width(72.dp)
                .clickable {
                    onClick()
                },

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Icon(

            imageVector =
                icon,

            contentDescription =
                title,

            tint =
                if (selected) {
                    Green
                } else {
                    TextTertiary
                },

            modifier =
                Modifier.size(22.dp)
        )

        Spacer(
            modifier =
                Modifier.height(4.dp)
        )

        Text(

            text =
                title,

            color =
                if (selected) {
                    Green
                } else {
                    TextTertiary
                },

            fontSize =
                10.sp
        )
    }
}