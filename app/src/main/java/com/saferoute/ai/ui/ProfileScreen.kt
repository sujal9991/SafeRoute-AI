package com.saferoute.ai.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saferoute.ai.core.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = Background,

        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profile",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                },

                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            imageVector =
                                Icons.Default.ArrowBack,

                            contentDescription =
                                "Back",

                            tint =
                                TextPrimary
                        )
                    }
                },

                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor =
                            Background
                    )
            )
        },

        bottomBar = {
            CleanBottomBar(
                selected = "Profile",
                onSelected = {}
            )
        }
    ) { paddingValues ->

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(20.dp)
        ) {

            Box(
                modifier =
                    Modifier
                        .size(70.dp)
                        .clip(
                            RoundedCornerShape(22.dp)
                        )
                        .background(
                            SurfaceColor
                        )
                        .then(
                            Modifier
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Person,

                    contentDescription =
                        null,

                    tint =
                        Green,

                    modifier =
                        Modifier.size(35.dp)
                )
            }

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            Text(
                text =
                    "SafeRoute AI",

                color =
                    TextPrimary,

                fontSize =
                    24.sp,

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )

            Text(
                text =
                    "Personalized safety-aware navigation",

                color =
                    TextSecondary,

                fontSize =
                    13.sp
            )

            Spacer(
                modifier =
                    Modifier.height(28.dp)
            )

            Card(
                modifier =
                    Modifier.fillMaxWidth(),

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

                Column(
                    modifier =
                        Modifier.padding(17.dp)
                ) {

                    Text(
                        text =
                            "About SafeRoute AI",

                        color =
                            TextPrimary,

                        fontSize =
                            16.sp,

                        fontWeight =
                            FontWeight.SemiBold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            "SafeRoute AI is designed to consider your vehicle, route conditions, weather and other available information to help you choose safer routes.",

                        color =
                            TextSecondary,

                        fontSize =
                            13.sp,

                        lineHeight =
                            19.sp
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )

            Card(
                modifier =
                    Modifier.fillMaxWidth(),

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

                    Icon(
                        imageVector =
                            Icons.Default.Person,

                        contentDescription =
                            null,

                        tint =
                            Green
                    )

                    Spacer(
                        modifier =
                            Modifier.width(12.dp)
                    )

                    Column {

                        Text(
                            text =
                                "Driver profile",

                            color =
                                TextPrimary,

                            fontSize =
                                14.sp,

                            fontWeight =
                                FontWeight.SemiBold
                        )

                        Spacer(
                            modifier =
                                Modifier.height(3.dp)
                        )

                        Text(
                            text =
                                "Your vehicle information will be used for personalized routing.",

                            color =
                                TextSecondary,

                            fontSize =
                                12.sp
                        )
                    }
                }
            }
        }
    }
}