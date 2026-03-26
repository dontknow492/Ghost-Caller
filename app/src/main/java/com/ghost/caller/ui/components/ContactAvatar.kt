package com.ghost.caller.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.ghost.caller.models.ContactType
import kotlin.math.absoluteValue

@Composable
fun ContactAvatar(
    photoUri: Uri?,
    initials: String,
    contactType: ContactType,
    size: Dp
) {
    val scheme = MaterialTheme.colorScheme

    val bgColor = when (contactType) {
        ContactType.GOOGLE -> scheme.primaryContainer
        ContactType.SIM -> scheme.secondaryContainer
        ContactType.WHATSAPP -> scheme.tertiaryContainer

        // 🔥 Dynamic color instead of boring default
        else -> generateColorFromName(initials, scheme)
    }

    val textColor = when (contactType) {
        ContactType.GOOGLE -> scheme.onPrimaryContainer
        ContactType.SIM -> scheme.onSecondaryContainer
        ContactType.WHATSAPP -> scheme.onTertiaryContainer
        else -> scheme.onSurface
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor)
            .border(
                width = 1.dp,
                color = scheme.outline.copy(alpha = 0.15f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {

        if (photoUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photoUri)
                    .crossfade(true)
                    .build(),
                contentDescription = "Contact Photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {

            val fontSize = when {
                size < 40.dp -> 12.sp
                size < 56.dp -> 14.sp
                else -> 18.sp
            }

            Text(
                text = initials.take(2).uppercase(),
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}


fun generateColorFromName(name: String, scheme: ColorScheme): Color {
    val colors = listOf(
        scheme.primaryContainer,
        scheme.secondaryContainer,
        scheme.tertiaryContainer,
        scheme.errorContainer,
        scheme.surfaceVariant
    )

    val index = (name.hashCode().absoluteValue) % colors.size
    return colors[index]
}