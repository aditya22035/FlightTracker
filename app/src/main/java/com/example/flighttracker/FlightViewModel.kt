package com.example.flighttracker

import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okio.IOException

private const val DEFAULT_REFRESH_INTERVAL = 300000L


class FlightViewModel(private val repository: FlightRepository):ViewModel(){
  private val _flightNumberInput = mutableStateOf("")
  val flightNumberInput: State<String> = _flightNumberInput

  private val _flightState = MutableStateFlow<LiveInfo?>(null)
  val flightState: StateFlow<LiveInfo?> = _flightState.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading:StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _errorMessage = MutableStateFlow<String?>(null)
  val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

  // --- Tracking Logic ---
  var trackingJob: Job? = null

  fun onFlightNumberChange(newInput: String){
    _flightNumberInput.value = newInput.uppercase().trim()
    if(trackingJob?.isActive == true)
      stopTracking()
    _errorMessage.value = null
    _flightState.value = null
  }
  fun stopTracking(){
    trackingJob?.cancel()
    trackingJob = null
    _isLoading.value = false
    println("tracking stopped")
  }


  private fun handleTrackingError(error: Throwable, flightNum: String){
    _flightState.value = null
    when(error){
      is FlightNotFoundException ->{
        _errorMessage.value = error.message ?: "Flight '$flightNum' not found."
      }
      is IOException -> {
        _errorMessage.value = "Network error. Please check connection. (${error.message})"
      }
      else ->{
        _errorMessage.value = "An unexpected error occurred: ${error.message}"
      }
    }
    _isLoading.value = false
  }
  override fun onCleared(){
    super.onCleared()
    trackingJob?.cancel()
    println("ViewModel cleared, tracking cancelled")
  }

  fun startTracking() {
    val flightNum = _flightNumberInput.value.trim()

    // **(5) Validation of user input**
    if (flightNum.isBlank()) {
      _errorMessage.value = "Please enter a flight number."
      return
    }

    // Cancel any existing tracking job
    trackingJob?.cancel()

    // Start a new tracking coroutine
    trackingJob = viewModelScope.launch {
      _isLoading.value = true
      _errorMessage.value = null // Clear previous errors
      _flightState.value = null // Clear previous data

      while (isActive) { // Loop for minute-by-minute updates
        // **(1) Utilizing API & Downloading** (via Repository)
        val result = repository.getFlightLiveData(flightNum)

        _isLoading.value = false // Loading finished for this iteration

        // **(3) Parsing JSON** (Handled by Retrofit/Moshi & Repository)

        result.fold(
          onSuccess = { data ->
            // **(4) Proper Output** (Update StateFlow)
            _flightState.value = data // Update the state with new data
            _errorMessage.value = null // Clear error message on success
            if (data == null) {
              // Handle case where success means "found nothing"
              _errorMessage.value = "Flight '$flightNum' data not currently available."
              stopTracking() // Stop if we get null data successfully
            }
          },
          onFailure = { error ->
            // **(5) Proper Error Messages**
            handleTrackingError(error, flightNum)
            // Stop tracking on error
            stopTracking() // Call internal stop without resetting input
          }
        )

        // Wait for 1 minute (60000ms) before the next fetch, only if job is still active
        if (isActive) {
          try {
            delay(60000L)
            _isLoading.value = true // Set loading for next iteration
          } catch (e: CancellationException) {
            // Job was cancelled (e.g., by stopTracking or ViewModel clearing)
            _isLoading.value = false
            println("Tracking delay cancelled.")
            break // Exit loop
          }
        }
      }
    }
  }

  // --- State for Average Calculation ---
  private val _averageDuration = MutableStateFlow<Double?>(null) // Holds average in minutes
  val averageDuration: StateFlow<Double?> = _averageDuration.asStateFlow()

  private val _isCalculating = MutableStateFlow(false)
  val isCalculating: StateFlow<Boolean> = _isCalculating.asStateFlow()

  // Function called by UI to trigger calculation (using hardcoded route for now)
  fun fetchAverageFlightTime() {
    // Example: Using the hardcoded route from the worker
    val origin = FlightDataCollectorWorker.ORIGIN_IATA
    val destination = FlightDataCollectorWorker.DESTINATION_IATA

    viewModelScope.launch {
      _isCalculating.value = true
      _errorMessage.value = null // Clear previous errors
      try {
        val avg = repository.calculateAverageDurationMinutes(origin, destination)
        _averageDuration.value = avg // Update state with result (can be null)
        if (avg == null) {
          _errorMessage.value = "Not enough data to calculate average time for $origin -> $destination."
        }
      } catch (e: Exception) {
        println("Error calculating average time: ${e.message}")
        _errorMessage.value = "Error calculating average time."
        _averageDuration.value = null
      } finally {
        _isCalculating.value = false
      }
    }
  }






  companion object {
    val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: CreationExtras
      ): T {
        // 1. Get Application Context
        val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])

        // 2. Get DAO instance
        val dao = AppDatabase.getDatabase(application).flightRecordDao()

        // 3. Get FlightApiService instance (e.g., from RetrofitClient singleton)
        val apiService = RetrofitClient.instance // <<< GET API SERVICE INSTANCE

        // 4. Create Repository with BOTH dependencies
        val repository = FlightRepository(
          flightDao = dao,         // Pass DAO
          apiService = apiService  // <<< PASS API SERVICE HERE
        )

        // 5. Create ViewModel with the Repository
        return FlightViewModel(repository) as T
      }
    }
  }





}