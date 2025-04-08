package com.example.flighttracker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.flighttracker.BuildConfig
import com.example.flighttracker.AppDatabase
import com.example.flighttracker.FlightRecord
import com.example.flighttracker.FlightApiService
import com.example.flighttracker.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

class FlightDataCollectorWorker(
  appContext: Context,
  workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

  // Access DAO and API Service (basic instantiation, consider DI)
  private val flightDao = AppDatabase.getDatabase(appContext).flightRecordDao()
  private val apiService: FlightApiService = RetrofitClient.instance
  private val apiKey = BuildConfig.AVIATIONSTACK_API_KEY

  companion object {
    const val WORK_NAME = "FlightDataCollectorWorker"
    // Define the route we want to track (example: Delhi to Mumbai)
    const val ORIGIN_IATA = "DEL"
    const val DESTINATION_IATA = "BOM"
    const val DAYS_TO_FETCH = 7 // How many past days to check (adjust as needed)
    const val FLIGHTS_PER_DAY_TARGET = 3 // Target number of flights to store per day
  }

  override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
    println("Worker: Starting flight data collection...")
    if (apiKey.isBlank() || apiKey == "\"\"") {
      println("Worker: Error - API Key missing.")
      return@withContext Result.failure()
    }

    try {
      // Fetch data for the last 'DAYS_TO_FETCH' days (e.g., yesterday back to 7 days ago)
      val today = LocalDate.now()
      for (i in 1..DAYS_TO_FETCH) {
        val targetDate = today.minusDays(i.toLong())
        val dateString = targetDate.format(DateTimeFormatter.ISO_LOCAL_DATE) // YYYY-MM-DD

        // Check if we already have enough data for this date to avoid redundant API calls
        val existingRecordsCount = flightDao.getFlightsForRouteOnDate(ORIGIN_IATA, DESTINATION_IATA, dateString).size
        if (existingRecordsCount >= FLIGHTS_PER_DAY_TARGET) {
          println("Worker: Skipping $dateString, already have $existingRecordsCount records.")
          continue
        }

        println("Worker: Fetching flights for $ORIGIN_IATA -> $DESTINATION_IATA on $dateString")

        val response = apiService.getLandedFlightsByRoute(
          apiKey = apiKey,
          departureIata = ORIGIN_IATA,
          arrivalIata = DESTINATION_IATA,
          flightDate = dateString,
          limit = FLIGHTS_PER_DAY_TARGET + 2 // Fetch slightly more to account for filtering
        )

        if (response.isSuccessful) {
          val flightDataList = response.body()?.data ?: emptyList()
          println("Worker: Found ${flightDataList.size} flights for $dateString.")

          val recordsToInsert = mutableListOf<FlightRecord>()
          var countForDay = existingRecordsCount // Start with existing count

          for (flightData in flightDataList) {
            if (countForDay >= FLIGHTS_PER_DAY_TARGET) break // Stop if target reached

            // Validate essential data for storage and calculation
            if (flightData.flight?.iata != null &&
              flightData.departure?.iata != null &&
              flightData.arrival?.iata != null &&
              flightData.departure.scheduled != null && // Need scheduled/actual times
              flightData.arrival.scheduled != null
            ) {
              // Convert timestamps to UTC epoch milliseconds
              val scheduledDepartureMillis = parseIsoToUtcMillis(flightData.departure.scheduled)
              val actualDepartureMillis = parseIsoToUtcMillis(flightData.departure.actual ?: flightData.departure.estimated ?: flightData.departure.scheduled) // Fallback logic for actual times
              val scheduledArrivalMillis = parseIsoToUtcMillis(flightData.arrival.scheduled)
              val actualArrivalMillis = parseIsoToUtcMillis(flightData.arrival.actual ?: flightData.arrival.estimated ?: flightData.arrival.scheduled) // Fallback logic for actual times

              // Only create record if essential times are parseable
              if (scheduledDepartureMillis != null && scheduledArrivalMillis != null) {
                val record = FlightRecord(
                  flightIata = flightData.flight.iata,
                  originIata = flightData.departure.iata,
                  destinationIata = flightData.arrival.iata,
                  flightDate = flightData.flightDate ?: dateString, // Use API date or target date
                  scheduledDepartureUtcMillis = scheduledDepartureMillis,
                  actualDepartureUtcMillis = actualDepartureMillis, // Can be null
                  scheduledArrivalUtcMillis = scheduledArrivalMillis,
                  actualArrivalUtcMillis = actualArrivalMillis     // Can be null
                )
                recordsToInsert.add(record)
                countForDay++
              } else {
                println("Worker: Skipping flight ${flightData.flight.iata} due to unparseable scheduled times.")
              }
            } else {
              println("Worker: Skipping flight due to missing essential IATA or scheduled time data.")
            }
          }

          if (recordsToInsert.isNotEmpty()) {
            flightDao.insertAll(recordsToInsert)
            println("Worker: Inserted ${recordsToInsert.size} records for $dateString.")
          }

        } else {
          // Handle API error (log it, maybe retry later depending on code)
          val errorBody = response.errorBody()?.string() ?: "Unknown API error"
          println("Worker: API Error (${response.code()}) fetching flights for $dateString: $errorBody")
          // Consider returning Result.retry() based on the error code
          if (response.code() == 429) { // Rate limit
            return@withContext Result.retry()
          }
          // Continue to next day on other errors for now
        }
        // Add a small delay between fetching dates to avoid hitting rate limits too fast
        kotlinx.coroutines.delay(1000L) // 1 second delay
      } // End for loop (days)

      // Optional: Cleanup old data (e.g., older than 7 days)
      val sevenDaysAgoMillis = System.currentTimeMillis() - (DAYS_TO_FETCH * 24 * 60 * 60 * 1000L)
      val deletedCount = flightDao.deleteRecordsOlderThan(sevenDaysAgoMillis)
      if (deletedCount > 0) {
        println("Worker: Cleaned up $deletedCount old records.")
      }

      println("Worker: Flight data collection finished successfully.")
      return@withContext Result.success()

    } catch (e: Exception) {
      println("Worker: Exception during work: ${e.message}")
      e.printStackTrace()
      // Return failure or retry based on the exception
      return@withContext Result.failure() // Or Result.retry()
    }
  }

  // Helper function to parse ISO 8601 timestamp string to UTC Epoch Milliseconds
  private fun parseIsoToUtcMillis(timestamp: String?): Long? {
    if (timestamp == null) return null
    return try {
      // Parse the timestamp assuming it includes offset or is implicitly UTC ('Z')
      OffsetDateTime.parse(timestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        .toInstant() // Convert to UTC Instant
        .toEpochMilli() // Get epoch milliseconds
    } catch (e: DateTimeParseException) {
      println("Worker: Failed to parse timestamp: $timestamp - ${e.message}")
      null // Return null if parsing fails
    }
  }
}