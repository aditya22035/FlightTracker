package com.example.flighttracker
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface FlightApiService {
  @GET("flights")
  suspend fun getFlightsByIata(
    @Query("access_key")apikey: String,
    @Query("flight_iata")flightIata: String,
    @Query("limit")limit:Int = 5
  ):Response<AviationStackResponse>

  @GET("flights")
  suspend fun getLandedFlightsByRoute(
    @Query("access_key") apiKey: String,
    @Query("dep_iata") departureIata: String,     // Origin airport IATA
    @Query("arr_iata") arrivalIata: String,       // Destination airport IATA
    @Query("flight_status") flightStatus: String = "landed", // Filter by status
    @Query("flight_date") flightDate: String,      // Specify date YYYY-MM-DD
    @Query("limit") limit: Int = 10 // Get a few results per day
  ): Response<AviationStackResponse>

  companion object{
    const val BASE_URL = "https://api.aviationstack.com/v1/"
  }
}