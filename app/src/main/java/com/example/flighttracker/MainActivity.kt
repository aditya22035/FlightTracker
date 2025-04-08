package com.example.flighttracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
// Assuming theme is in ui.theme package
import com.example.flighttracker.ui.theme.FlightTrackerTheme
// Assuming screen is directly in ui package or this package
import com.example.flighttracker.FlightScreen // Adjust import if needed

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      FlightTrackerTheme { // Apply your application's theme
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          // Set the main screen Composable as the content
          FlightScreen()
        }
      }
    }
  }
}