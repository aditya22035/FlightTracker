package com.example.flighttracker

import android.app.Application
import android.util.Log // Import Android Log
import androidx.work.*
// Import necessary classes for seeding (adjust package if needed)
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

// Register this class in AndroidManifest.xml's <application> tag using android:name=".MainApplication"
class MainApplication : Application() {

  // A scope for application-level background tasks like seeding
  private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override fun onCreate() {
    super.onCreate()

    // 1. Schedule the real background worker (runs periodically)
    enqueueFlightDataCollectionWork()

    // 2. Initiate sample data seeding (runs off main thread, checks if needed)
    seedDatabaseWithSampleDataIfNeeded()
  }

  // Schedules the periodic WorkManager job for ongoing data collection
  private fun enqueueFlightDataCollectionWork() {
    val constraints = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .build()

    val periodicWorkRequest = PeriodicWorkRequestBuilder<FlightDataCollectorWorker>(
      repeatInterval = 1,
      repeatIntervalTimeUnit = TimeUnit.DAYS
    )
      .setConstraints(constraints)
      .setBackoffCriteria(
        backoffPolicy = BackoffPolicy.LINEAR,
        backoffDelay = WorkRequest.MIN_BACKOFF_MILLIS,
        timeUnit = TimeUnit.MILLISECONDS
      )
      .build()

    WorkManager.getInstance(this).enqueueUniquePeriodicWork(
      FlightDataCollectorWorker.WORK_NAME,
      ExistingPeriodicWorkPolicy.KEEP, // Keeps existing schedule if already running
      periodicWorkRequest
    )

    Log.i("MainApplication", "Periodic flight data collection work enqueued.") // Use Logcat
  }

  // --- Sample Data Seeding Logic ---

  // Initiates the seeding process in a background coroutine
  private fun seedDatabaseWithSampleDataIfNeeded() {
    applicationScope.launch {
      val dao = AppDatabase.getDatabase(this@MainApplication).flightRecordDao()

      // Check if sample data likely exists already to avoid re-inserting every time.
      // We check based on a specific route/date combination used in the sample data.
      val dateToCheck = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
      val existingRecords = dao.getFlightsForRouteOnDate(
        FlightDataCollectorWorker.ORIGIN_IATA, // "DEL"
        FlightDataCollectorWorker.DESTINATION_IATA, // "BOM"
        dateToCheck
      )

      // Only insert if no records matching the check criteria are found
      if (existingRecords.isEmpty()) {
        Log.i("MainApplication", "Seeding database with sample DEL->BOM flight data...")
        val sampleRecords = createSampleFlightRecords()
        dao.insertAll(sampleRecords)
        Log.i("MainApplication", "Sample flight data seeded.")
      } else {
        Log.i("MainApplication", "Sample flight data already exists, skipping seed.")
      }
    }
  }

  // Creates a list of sample FlightRecord objects for demonstration
  private fun createSampleFlightRecords(): List<FlightRecord> {
    val records = mutableListOf<FlightRecord>()
    val dateStr = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE) // Use yesterday's date string

    // Sample 1: ~105 mins duration
    records.add(
      FlightRecord(
        flightIata = "6E201", originIata = "DEL", destinationIata = "BOM", flightDate = dateStr,
        scheduledDepartureUtcMillis = dtMillis(2025, 4, 8, 6, 0), // Apr 8, 6:00 UTC (using current date context)
        actualDepartureUtcMillis = dtMillis(2025, 4, 8, 6, 10),
        scheduledArrivalUtcMillis = dtMillis(2025, 4, 8, 7, 45),
        actualArrivalUtcMillis = dtMillis(2025, 4, 8, 7, 55)
      )
    )
    // Sample 2: ~95 mins duration
    records.add(
      FlightRecord(
        flightIata = "AI887", originIata = "DEL", destinationIata = "BOM", flightDate = dateStr,
        scheduledDepartureUtcMillis = dtMillis(2025, 4, 8, 8, 0),
        actualDepartureUtcMillis = dtMillis(2025, 4, 8, 8, 5),
        scheduledArrivalUtcMillis = dtMillis(2025, 4, 8, 9, 45),
        actualArrivalUtcMillis = dtMillis(2025, 4, 8, 9, 40)
      )
    )
    // Sample 3: ~110 mins duration
    records.add(
      FlightRecord(
        flightIata = "UK951", originIata = "DEL", destinationIata = "BOM", flightDate = dateStr,
        scheduledDepartureUtcMillis = dtMillis(2025, 4, 8, 10, 0),
        actualDepartureUtcMillis = dtMillis(2025, 4, 8, 10, 15),
        scheduledArrivalUtcMillis = dtMillis(2025, 4, 8, 11, 50),
        actualArrivalUtcMillis = dtMillis(2025, 4, 8, 12, 5)
      )
    )
    // Sample 4: Record with missing actual arrival (ignored by calculation)
    records.add(
      FlightRecord(
        flightIata = "SG160", originIata = "DEL", destinationIata = "BOM", flightDate = dateStr,
        scheduledDepartureUtcMillis = dtMillis(2025, 4, 8, 14, 0),
        actualDepartureUtcMillis = dtMillis(2025, 4, 8, 14, 5),
        scheduledArrivalUtcMillis = dtMillis(2025, 4, 8, 15, 55),
        actualArrivalUtcMillis = null
      )
    )
    return records
  }

  // Helper to create UTC epoch milliseconds from date/time components easily
  private fun dtMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
    // Using current year, adjust if needed for specific date examples
    return OffsetDateTime.of(year, month, day, hour, minute, 0, 0, ZoneOffset.UTC)
      .toInstant()
      .toEpochMilli()
  }
  // --- End Sample Data Seeding ---
}