package com.ghost.caller.ui.components

import android.app.WallpaperManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberWallpaperBitmap(): ImageBitmap? {
    val context = LocalContext.current
    var wallpaperBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(Unit) {
        // Move the heavy image decoding to a background thread
        withContext(Dispatchers.IO) {
            try {
                // NOTE: This will still fail on Android 13+ or without permissions
                val drawable = WallpaperManager.getInstance(context).drawable
                wallpaperBitmap = drawable?.toBitmap()?.asImageBitmap()
            } catch (e: SecurityException) {
                // Log the exception so it doesn't fail silently in the future
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    return wallpaperBitmap
}