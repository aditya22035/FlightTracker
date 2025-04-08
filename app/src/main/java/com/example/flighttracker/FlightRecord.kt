package com.example.flighttracker
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
  tableName = "flight_history",
  // Add indices for faster querying by route and date
  indices = [Index(value = ["originIata", "destinationIata"]), Index(value = ["flightDate"])]
)
data class FlightRecord (
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val flightIata: String,
  val originIata: String,
  val destinationIata: String,
  val flightDate: String,
  // Store timestamps as Epoch Milliseconds (UTC) for accurate duration calculation
  val scheduledDepartureUtcMillis: Long?,
  val actualDepartureUtcMillis: Long?,
  val scheduledArrivalUtcMillis: Long?,
  val actualArrivalUtcMillis: Long?,

  val recordedAtMillis: Long = System.currentTimeMillis() // Timestamp when record was added
){
  val actualDurationMinutes: Long?
    get() {
      return if (actualArrivalUtcMillis != null && actualDepartureUtcMillis != null) {
        (actualArrivalUtcMillis - actualDepartureUtcMillis) / (1000 * 60) // Duration in minutes
      } else {
        null
      }
    }
}