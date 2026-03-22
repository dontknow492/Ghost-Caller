package com.ghost.caller

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ghost.caller.ui.screens.CallingApp
import com.ghost.caller.ui.theme.CallerTheme
import com.ghost.caller.viewmodel.CallViewModel

// --- ENTRY POINT ---
// ---------------------------
// Main Activity
// ---------------------------
class MainActivity : ComponentActivity() {

    private lateinit var viewModel: CallViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        viewModel = ViewModelProvider(
            this,
            CallViewModelFactory(application)
        ).get(CallViewModel::class.java)

        setContent {
            CallerTheme(
                isDarkTheme = isSystemInDarkTheme(),
                dynamicColor = true
            ) {

                CallingApp(viewModel = viewModel)
            }

        }
    }
}


class CallViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CallViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CallViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}