package com.example.flightsearch.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Airport::class, Favorite::class],
    version = 1,
    exportSchema = false
)
abstract class FlightDatabase : RoomDatabase() {
    abstract fun airportDao(): AirportDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile
        private var INSTANCE: FlightDatabase? = null

        fun getDatabase(context: Context): FlightDatabase {
            return INSTANCE ?: synchronized(this) {
                try {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        FlightDatabase::class.java,
                        "flight_search.db"
                    )
                        .createFromAsset("flight_search.db")
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                    instance
                } catch (e: Exception) {
                    Log.e("FlightDatabase", "Error creating database", e)
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        FlightDatabase::class.java,
                        "flight_search.db"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                    instance
                }
            }
        }
    }
}