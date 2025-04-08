package com.example.flighttracker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FlightRecordDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertFlightRecord(record: FlightRecord)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(records: List<FlightRecord>)

  @Query("SELECT * FROM flight_history WHERE originIata = :origin AND destinationIata = :destination ORDER BY flightDate DESC")
  suspend fun getFlightsBetween(origin: String, destination: String): List<FlightRecord>//flights between two specific airports

  @Query("SELECT * FROM flight_history WHERE originIata = :origin AND destinationIata = :destination AND flightDate = :date")
  suspend fun getFlightsForRouteOnDate(origin: String, destination: String, date: String): List<FlightRecord>//specific airports, specific date

  @Query("SELECT * FROM flight_history ORDER BY recordedAtMillis DESC")
  suspend fun getAllRecords(): List<FlightRecord>

  @Query("DELETE FROM flight_history WHERE recordedAtMillis < :timestampMillis")
  suspend fun deleteRecordsOlderThan(timestampMillis: Long): Int // Returns number of rows deleted


  @Query("SELECT * FROM flight_history WHERE originIata = :origin AND destinationIata = :destination " +
      "AND actualDepartureUtcMillis IS NOT NULL AND actualArrivalUtcMillis IS NOT NULL")
  suspend fun getValidFlightsForAverage(origin: String, destination: String): List<FlightRecord>

}