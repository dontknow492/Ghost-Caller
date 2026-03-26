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

    // 🔥 Improved: Fetch both background and text color as a pair to ensure legibility
    val (bgColor, textColor) = when (contactType) {
        ContactType.GOOGLE -> scheme.primaryContainer to scheme.onPrimaryContainer
        ContactType.SIM -> scheme.secondaryContainer to scheme.onSecondaryContainer
        ContactType.WHATSAPP -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        else -> generateAvatarColors(initials, scheme)
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
                color = textColor // 🔥 Now using the matched accessible color
            )
        }
    }
}


/**
 * Generates a consistent pair of colors (background and content) based on a name string.
 * This ensures avatars have variety but the text inside remains readable.
 * Expanded to 18 color options for high variety in large contact lists.
 */
fun generateAvatarColors(name: String?, scheme: ColorScheme): Pair<Color, Color> {
    val safeName = name?.trim()?.ifEmpty { "Unknown" } ?: "Unknown"

    val colorOptions = listOf(
        // 1. Primary Container Pair
        scheme.primaryContainer to scheme.onPrimaryContainer,
        // 2. Secondary Container Pair
        scheme.secondaryContainer to scheme.onSecondaryContainer,
        // 3. Tertiary Container Pair
        scheme.tertiaryContainer to scheme.onTertiaryContainer,
        // 4. Error Container Pair
        scheme.errorContainer to scheme.onErrorContainer,
        // 5. Surface Variant Pair
        scheme.surfaceVariant to scheme.onSurfaceVariant,
        // 6. Inverse Primary Pair
        scheme.inversePrimary to scheme.onSurface,
        // 7. Secondary Pair
        scheme.secondary to scheme.onSecondary,
        // 8. Tertiary Pair
        scheme.tertiary to scheme.onTertiary,
        // 9. Primary Pair
        scheme.primary to scheme.onPrimary,
        // 10. Inverse Surface Pair
        scheme.inverseSurface to scheme.inverseOnSurface,
        // 11. Swapped Primary Container
        scheme.onPrimaryContainer to scheme.primaryContainer,
        // 12. Swapped Secondary Container
        scheme.onSecondaryContainer to scheme.secondaryContainer,
        // 13. Swapped Tertiary Container
        scheme.onTertiaryContainer to scheme.tertiaryContainer,
        // 14. Surface Tint Variant
        scheme.surfaceTint to scheme.onPrimary,
        // 15. Outline Variant
        scheme.outline to scheme.surface,
        // 16. On-Surface to Surface
        scheme.onSurface to scheme.surface,
        // 17. On-Surface-Variant to Surface-Variant
        scheme.onSurfaceVariant to scheme.surfaceVariant,
        // 18. Muted Secondary variant
        scheme.secondary.copy(alpha = 0.8f) to scheme.onSecondary
    )

    // Using absoluteValue to handle potential negative hashCodes
    val index = safeName.hashCode().absoluteValue % colorOptions.size
    return colorOptions[index]
}

/**
 * Legacy support for just the background color
 */
fun generateColorFromName(name: String, scheme: ColorScheme): Color {
    return generateAvatarColors(name, scheme).first
}