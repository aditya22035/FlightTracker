package com.example.flighttracker
import com.example.flighttracker.BuildConfig // Import generated BuildConfig
import com.example.flighttracker.FlightApiService
import com.example.flighttracker.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.IOException
import retrofit2.HttpException

class FlightRepository(
  // Inject API service, default to singleton RetrofitClient instance
  private val apiService: FlightApiService = RetrofitClient.instance,
  private val flightDao: FlightRecordDao
) {

  // Safely get API key from BuildConfig generated via local.properties
  private val apiKey = BuildConfig.AVIATIONSTACK_API_KEY

  // Fetches live flight data for a given IATA code
  suspend fun getFlightLiveData(flightIata: String): Result<LiveInfo?> {
    // Validate API Key presence
    if (apiKey.isBlank() || apiKey == "\"\"") { // BuildConfig adds quotes if key was empty in props
      return Result.failure(Exception("AviationStack API Key not configured in local.properties/BuildConfig."))
    }

    // Execute network call on the IO dispatcher
    return withContext(Dispatchers.IO) {
      try {
        val response = apiService.getFlightsByIata(apiKey, flightIata)

        if (response.isSuccessful) {
          val flightList = response.body()?.data
          if (flightList.isNullOrEmpty()) {
            Result.failure(FlightNotFoundException("No flight data found for '$flightIata'."))
          } else {
            // Prioritize active flights with live data
            val targetFlight = flightList.find { it.flightStatus == "active" && it.live != null }
              ?: flightList.find { it.live != null } // Fallback: any flight with live data

            if (targetFlight?.live != null) {
              Result.success(targetFlight.live) // Success: return LiveInfo
            } else {
              // Found flight(s), but none match criteria (e.g., scheduled, landed, no live feed)
              val firstStatus = flightList.firstOrNull()?.flightStatus ?: "unknown"
              Result.failure(FlightNotFoundException("Flight '$flightIata' found (Status: $firstStatus), but no live data is currently available."))
            }
          }
        } else {
          // API returned an error response (4xx, 5xx)
          val errorBody = response.errorBody()?.string() ?: "No details"
          val errorCode = response.code()
          var errorMsg = "API Error ($errorCode): ${response.message()}"

          // Check for common AviationStack error codes (refer to their docs)
          // Example codes (might change): 101=invalid key, 104=usage limit, etc.
          if (errorCode == 429 || errorBody.contains("usage limits")) {
            errorMsg = "API Rate Limit Exceeded. Please wait."
          } else if (errorCode == 401 || errorCode == 101 || errorBody.contains("invalid_access_key")) {
            errorMsg = "Invalid API Access Key."
          } else {
            errorMsg += "\nDetails: $errorBody" // Include details for other errors
          }
          Result.failure(IOException(errorMsg)) // Wrap API error in IOException or custom Exception
        }
      } catch (e: HttpException) {
        // Catch Retrofit-specific HTTP exceptions
        Result.failure(IOException("HTTP Error: ${e.code()} ${e.message()}", e))
      }
      catch (e: IOException) {
        // Catch network errors (connectivity, timeouts)
        Result.failure(IOException("Network Error: ${e.message}", e))
      } catch (e: Exception) {
        // Catch other potential errors (e.g., JSON parsing issues if API changes)
        Result.failure(Exception("An unexpected error occurred: ${e.message}", e))
      }
    }
  }
  suspend fun calculateAverageDurationMinutes(origin: String, destination: String): Double? = withContext(Dispatchers.IO) {
    println("Repository: Calculating average duration for $origin -> $destination")
    // Get only records with valid actual departure and arrival times
    val validRecords = flightDao.getValidFlightsForAverage(origin, destination)

    if (validRecords.isEmpty()) {
      println("Repository: No valid records found for $origin -> $destination to calculate average.")
      return@withContext null // No data to calculate average
    }

    val totalDurationMillis = validRecords.sumOf { record ->
      // We already queried for non-null values, but double-check for safety
      (record.actualArrivalUtcMillis ?: 0L) - (record.actualDepartureUtcMillis ?: 0L)
    }

    // Avoid division by zero
    if (validRecords.isNotEmpty()) {
      val averageMillis = totalDurationMillis.toDouble() / validRecords.size
      val averageMinutes = averageMillis / (1000.0 * 60.0) // Convert average millis to minutes
      println("Repository: Calculated average duration: %.2f minutes from %d records.".format(averageMinutes, validRecords.size))
      return@withContext averageMinutes
    } else {
      return@withContext null
    }
  }
}