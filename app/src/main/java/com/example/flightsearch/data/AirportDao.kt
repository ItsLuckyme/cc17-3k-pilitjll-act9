package com.example.flightsearch.data

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AirportDao {
    @Query("""SELECT * FROM airport WHERE name LIKE :searchQuery OR iata_code LIKE :searchQuery ORDER BY passengers DESC """)
    fun getAirportsBySearchQuery(searchQuery: String): Flow<List<Airport>>

    @Query("SELECT * FROM airport WHERE iata_code != :excludeCode ORDER BY passengers DESC")
    fun getAllAirportsExcept(excludeCode: String): Flow<List<Airport>>

    @Query("SELECT * FROM airport WHERE iata_code = :code LIMIT 1")
    suspend fun getAirportByCode(code: String): Airport?
}