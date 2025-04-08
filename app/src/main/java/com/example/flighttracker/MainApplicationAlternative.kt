package com.example.flighttracker

import android.app.Application
import android.util.Log // Use Android Log
import androidx.work.*
import java.util.concurrent.TimeUnit

// This is an alternative Application class that ONLY schedules the background worker.
// It does NOT seed the database with sample data.
// To use this, update android:name in AndroidManifest.xml to ".MainApplicationAlternative"
// OR rename this class/file to MainApplication.kt (replacing the seeding version).
class MainApplicationAlternative : Application() {

  override fun onCreate() {
    super.onCreate()
    // Only schedule the background work when the application starts
    enqueueFlightDataCollectionWork()
  }

  // Schedules the periodic WorkManager job for ongoing data collection
  private fun enqueueFlightDataCollectionWork() {
    // Define constraints for the worker (e.g., requires network)
    val constraints = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      // Add other constraints if needed (battery, storage, etc.)
      .build()

    // Build a periodic work request to run roughly once a day
    val periodicWorkRequest = PeriodicWorkRequestBuilder<FlightDataCollectorWorker>(
      repeatInterval = 1, // Interval: 1 day
      repeatIntervalTimeUnit = TimeUnit.DAYS
    )
      .setConstraints(constraints)
      // Define retry strategy if the worker fails
      .setBackoffCriteria(
        backoffPolicy = BackoffPolicy.LINEAR,
        backoffDelay = WorkRequest.MIN_BACKOFF_MILLIS, // Use WorkRequest constant
        timeUnit = TimeUnit.MILLISECONDS
      )
      .build()

    // Enqueue the work uniquely using KEEP policy.
    // This ensures that if the work is already scheduled, a new one isn't added.
    WorkManager.getInstance(this).enqueueUniquePeriodicWork(
      FlightDataCollectorWorker.WORK_NAME, // The unique name for this work sequence
      ExistingPeriodicWorkPolicy.KEEP, // Keep the existing work if it's already scheduled
      periodicWorkRequest
    )

    // Use Logcat for logging in Android apps
    Log.i("MainAppAlternative", "Periodic flight data collection work enqueued.")
  }
}