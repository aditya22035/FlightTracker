package com.example.flighttracker

import android.icu.text.DecimalFormat
// Use java.time for parsing ISO 8601 dates from API
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flighttracker.FlightViewModel
import com.example.flighttracker.LiveInfo
import java.time.format.DateTimeParseException

// **(2) UI Creation** (Main screen structure largely the same)
@Composable
fun FlightScreen(
  modifier: Modifier = Modifier,
  viewModel: FlightViewModel = viewModel(factory = FlightViewModel.Factory)
) {
  val flightNumberInput by viewModel.flightNumberInput
  // Observe the LiveInfo? state
  val liveInfoState by viewModel.flightState.collectAsStateWithLifecycle() // **Type changed**
  val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
  val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
  val isTracking = viewModel.trackingJob?.isActive ?: false

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Text("AviationStack Flight Tracker", style = MaterialTheme.typography.headlineMedium)

    OutlinedTextField(
      value = flightNumberInput,
      onValueChange = viewModel::onFlightNumberChange,
      // Update label for IATA code
      label = { Text("Enter Flight IATA (e.g., BA123)") },
      modifier = Modifier.fillMaxWidth(),
      singleLine = true,
      enabled = !isTracking
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceEvenly
    ) {
      Button(
        onClick = viewModel::startTracking,
        enabled = !isLoading && !isTracking && flightNumberInput.isNotBlank()
      ) {
        Text("Track Flight")
      }
      Button(
        onClick = viewModel::stopTracking,
        enabled = isTracking || isLoading
      ) {
        Text("Stop Tracking")
      }
    }

    if (isLoading && !isTracking) { // Show loading only when initially fetching or between intervals
      CircularProgressIndicator(modifier = Modifier.padding(top = 20.dp))
      Text("Fetching live data...", style = MaterialTheme.typography.bodySmall)
    }
    if(isLoading && isTracking){
      Text("Waiting for next update...", style = MaterialTheme.typography.bodySmall, modifier=Modifier.padding(top=20.dp))
    }

    // **(5) Error Messages**
    errorMessage?.let { message ->
      Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium, // Slightly smaller for potentially longer messages
        modifier = Modifier.padding(top = 10.dp)
      )
    }

    // **(4) Proper Output** (Display LiveInfo)
    liveInfoState?.let { liveInfo ->
      // Don't necessarily hide if loading, show last known state while waiting
      FlightDetailsCard(liveInfo = liveInfo)
    }

    if (liveInfoState == null && errorMessage == null && !isLoading) {
      Text("Enter a flight IATA code and press 'Track Flight'.", modifier = Modifier.padding(top = 20.dp))
    }


    // --- Section for Average Time Calculation ---
    Divider(modifier = Modifier.padding(vertical = 8.dp)) // Separator

    val averageDurationMinutes by viewModel.averageDuration.collectAsStateWithLifecycle()
    val isCalculating by viewModel.isCalculating.collectAsStateWithLifecycle()

    Button(
      onClick = { viewModel.fetchAverageFlightTime() },
      enabled = !isCalculating // Disable button while calculating
    ) {
      if (isCalculating) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp
          )
          Spacer(Modifier.width(8.dp))
          Text("Calculating...")
        }
      } else {
        // Use the hardcoded route for the button text (make dynamic later if needed)
        Text("Avg Time ${FlightDataCollectorWorker.ORIGIN_IATA} -> ${FlightDataCollectorWorker.DESTINATION_IATA}")
      }
    }

    // Display the calculated average time or status
    averageDurationMinutes?.let { avg ->
      Text(
        // Format the average minutes nicely
        text = "Average Flight Time: %.1f minutes".format(avg),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(top = 8.dp)
      )
    }
    // Display calculation-related errors separately if needed, or rely on general errorMessage state
    if (errorMessage?.contains("average time", ignoreCase = true) == true) {
      Text(
        text = errorMessage!!,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 8.dp)
      )
    }






  }



}

// Update Card to accept and display LiveInfo
@Composable
fun FlightDetailsCard(liveInfo: LiveInfo, modifier: Modifier = Modifier) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(top = 16.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Text("Live Flight Data", style = MaterialTheme.typography.titleMedium)
      Divider()

      // Display fields from LiveInfo. Check AviationStack docs for units!
      DetailRow("Latitude:", liveInfo.latitude?.format(4))
      DetailRow("Longitude:", liveInfo.longitude?.format(4))
      // Assuming Altitude is in feet, Speed in km/h based on typical AviationStack usage
      DetailRow("Altitude (feet):", liveInfo.altitude?.format(0))
      DetailRow("Ground Speed (km/h):", liveInfo.speedHorizontal?.format(1))
      DetailRow("Vertical Speed (km/h):", liveInfo.speedVertical?.format(1))
      DetailRow("Direction (degrees):", liveInfo.direction?.format(1))
      DetailRow("On Ground:", liveInfo.isGround?.toString())
      DetailRow("Last Updated (UTC):", liveInfo.updated?.let { formatIsoTimestamp(it) })
    }
  }
}

// DetailRow Composable remains the same
@Composable
fun DetailRow(label: String, value: String?) {
  Row(modifier = Modifier.fillMaxWidth()) {
    Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.width(180.dp)) // Wider label
    Text(value ?: "N/A")
  }
}


// --- Helper Functions ---

// Keep Double formatter
fun Double.format(digits: Int): String {
  return DecimalFormat().apply {
    maximumFractionDigits = digits
    minimumFractionDigits = digits
  }.format(this)
}

// Update timestamp formatter for ISO 8601 format (common in modern APIs)
fun formatIsoTimestamp(isoTimestamp: String): String {
  return try {
    // Parse the OffsetDateTime (includes timezone info like +00:00 or Z)
    val odt = OffsetDateTime.parse(isoTimestamp)
    // Define a desired output format
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.ENGLISH)
    odt.format(formatter)
  } catch (e: DateTimeParseException) {
    // Fallback for slightly different formats if needed
    try {
      val ldt = LocalDateTime.parse(isoTimestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
      val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
      ldt.format(formatter) + " (Timezone Unknown)"
    } catch (e2: DateTimeParseException){
      isoTimestamp // Return original string if parsing fails
    }
  } catch (e: Exception) {
    isoTimestamp // Return original string on other errors
  }
}

// --- Preview --- (Update preview if needed)
@Preview(showBackground = true)
@Composable
fun FlightTrackingScreenPreview() {
  MaterialTheme {
    FlightScreen()
  }
}