package com.ghost.caller.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghost.caller.R
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionRequestScreen(
    onRequestPermissions: () -> Unit,
    isRequesting: Boolean = false
) {
    val materialTheme = MaterialTheme
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600 ||
            configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(300)
        showContent = true
    }

    val infiniteTransition = rememberInfiniteTransition(label = "icon_scale")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    Scaffold(
        containerColor = materialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            materialTheme.colorScheme.primary,
                            materialTheme.colorScheme.primaryContainer
                        )
                    )
                )
        ) {
            // Added verticalScroll so smaller landscape phones don't cut off the button
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(animationSpec = tween(500)) +
                            slideInVertically(initialOffsetY = { it / 2 })
                ) {
                    if (isWideScreen) {
                        // --- WIDESCREEN / LANDSCAPE LAYOUT ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Left Side: Branding
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                PermissionHeaderSection(scale = scale)
                            }

                            // Right Side: Requirements & Actions
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1.2f)
                                    .padding(horizontal = 16.dp)
                            ) {
                                PermissionCardsAndActions(
                                    isRequesting = isRequesting,
                                    onRequestPermissions = onRequestPermissions
                                )
                            }
                        }
                    } else {
                        // --- STANDARD PORTRAIT LAYOUT ---
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            PermissionHeaderSection(scale = scale)
                            Spacer(modifier = Modifier.height(32.dp))
                            PermissionCardsAndActions(
                                isRequesting = isRequesting,
                                onRequestPermissions = onRequestPermissions
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------
// EXTRACTED COMPONENT 1: The Icon and Title
// ---------------------------------------------------------
@Composable
private fun PermissionHeaderSection(scale: Float) {
    val materialTheme = MaterialTheme
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(materialTheme.colorScheme.onPrimary.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Phone,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .scale(scale),
            tint = materialTheme.colorScheme.onPrimary
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = stringResource(R.string.app_name),
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        color = materialTheme.colorScheme.onPrimary,
        style = MaterialTheme.typography.headlineMedium
    )
}

// ---------------------------------------------------------
// EXTRACTED COMPONENT 2: Cards and Buttons
// ---------------------------------------------------------
@Composable
private fun PermissionCardsAndActions(
    isRequesting: Boolean,
    onRequestPermissions: () -> Unit
) {
    val materialTheme = MaterialTheme

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PermissionRequirementCard(
            icon = Icons.Default.Call,
            title = "Phone Calls",
            description = "To make and receive calls",
            color = materialTheme.colorScheme.onPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        PermissionRequirementCard(
            icon = ImageVector.vectorResource(R.drawable.contacts_24px),
            title = "Contacts Access",
            description = "To show contact names",
            color = materialTheme.colorScheme.onPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        PermissionRequirementCard(
            icon = ImageVector.vectorResource(R.drawable.history_24px),
            title = "Call Log Access",
            description = "To display recent calls",
            color = materialTheme.colorScheme.onPrimary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRequestPermissions,
            enabled = !isRequesting,
            colors = ButtonDefaults.buttonColors(
                containerColor = materialTheme.colorScheme.onPrimary,
                contentColor = materialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (isRequesting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = materialTheme.colorScheme.primary
                )
            } else {
                Text("Grant Permissions", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun PermissionRequirementCard(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color
) {
    val materialTheme = MaterialTheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = materialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with circular background
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(materialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = color,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = description,
                    color = color.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Check icon
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = color.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// Optional: Add a shimmer effect for loading state
@Composable
fun PermissionRequestScreenShimmer(onRequestPermissions: () -> Unit) {
    val materialTheme = MaterialTheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        materialTheme.colorScheme.primary,
                        materialTheme.colorScheme.primaryContainer
                    )
                )
            )
    ) {
        // Shimmer effect implementation
        // Can be added for loading states
    }
}