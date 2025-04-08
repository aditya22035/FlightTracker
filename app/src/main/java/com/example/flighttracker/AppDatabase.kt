package com.example.flighttracker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities = [FlightRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase: RoomDatabase() {
  abstract fun flightRecordDao(): FlightRecordDao

  companion object{
    @Volatile
    private var INSTANCE: AppDatabase?=null

    fun getDatabase(context: Context): AppDatabase{
      return INSTANCE?: synchronized(this){
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "flight_tracker_database"
        )
          .fallbackToDestructiveMigrationFrom()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}