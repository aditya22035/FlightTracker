package com.example.flighttracker
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AviationStackResponse(
  @Json(name = "pagination")val pagination: Pagination?,
  @Json(name = "data")val data: List<FlightData>?
)

@JsonClass(generateAdapter = true)
data class FlightData(
  @Json(name = "flight_date") val flightDate: String?,
  @Json(name = "flight_status") val flightStatus: String?, // e.g., "scheduled", "active", "landed"
  @Json(name = "departure") val departure: DepartureArrivalInfo?,
  @Json(name = "arrival") val arrival: DepartureArrivalInfo?,
  @Json(name = "airline") val airline: AirlineInfo?,
  @Json(name = "flight") val flight: FlightInfo?,
  @Json(name = "aircraft") val aircraft: AircraftInfo?,
  // The crucial part for real-time tracking (can be null)
  @Json(name = "live") val live: LiveInfo?
)


@JsonClass(generateAdapter = true)
data class Pagination(
  @Json(name = "limit")val limit: Int?,
  @Json(name = "offset")val offset: Int?,
  @Json(name = "count")val count: Int?,
  @Json(name = "total")val total: Int?,
)

@JsonClass(generateAdapter = true)
data class DepartureArrivalInfo(
  @Json(name = "airport") val airport: String?,
  @Json(name = "timezone") val timezone: String?,
  @Json(name = "iata") val iata: String?,
  @Json(name = "icao") val icao: String?,
  @Json(name = "terminal") val terminal: String?,
  @Json(name = "gate") val gate: String?,
  @Json(name = "delay") val delay: Int?,
  @Json(name = "scheduled") val scheduled: String?,
  @Json(name = "estimated") val estimated: String?,
  @Json(name = "actual") val actual: String?
)

@JsonClass(generateAdapter = true)
data class AirlineInfo(
  @Json(name = "name") val name: String?,
  @Json(name = "iata") val iata: String?,
  @Json(name = "icao") val icao: String?
)

@JsonClass(generateAdapter = true)
data class FlightInfo(
  @Json(name = "number") val number: String?,
  @Json(name = "iata") val iata: String?,
  @Json(name = "icao") val icao: String?,
  @Json(name = "codeshared") val codeshared: CodesharedInfo? // Changed to nullable object
)
@JsonClass(generateAdapter = true)
data class CodesharedInfo(
  @Json(name = "airline_name") val airlineName: String?,
  @Json(name = "airline_iata") val airlineIata: String?,
  @Json(name = "airline_icao") val airlineIcao: String?,
  @Json(name = "flight_number") val flightNumber: String?,
  @Json(name = "flight_iata") val flightIata: String?,
  @Json(name = "flight_icao") val flightIcao: String?
)

@JsonClass(generateAdapter = true)
data class AircraftInfo(
  @Json(name = "registration") val registration: String?,
  @Json(name = "iata") val iata: String?,
  @Json(name = "icao") val icao: String?,
  @Json(name = "icao24") val icao24: String?
)

@JsonClass(generateAdapter = true)
data class LiveInfo(
  @Json(name = "updated") val updated: String?, // ISO 8601 timestamp
  @Json(name = "latitude") val latitude: Double?,
  @Json(name = "longitude") val longitude: Double?,
  @Json(name = "altitude") val altitude: Double?, // Likely feet
  @Json(name = "direction") val direction: Double?, // Degrees
  @Json(name = "speed_horizontal") val speedHorizontal: Double?, // Likely km/h
  @Json(name = "speed_vertical") val speedVertical: Double?, // Likely km/h
  @Json(name = "is_ground") val isGround: Boolean?
)

// Custom exception class (can stay in this file or move to its own)
class FlightNotFoundException(message: String) : Exception(message)

