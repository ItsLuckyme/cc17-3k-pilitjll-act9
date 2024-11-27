package com.example.flightsearch.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite")
    fun getAllFavorites(): Flow<List<Favorite>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: Favorite)

    @Delete
    suspend fun deleteFavorite(favorite: Favorite)

    @Query("""SELECT EXISTS(SELECT 1 FROM favorite WHERE departure_code = :departureCode AND destination_code = :destinationCode)""")
    fun isFavorite(departureCode: String, destinationCode: String): Flow<Boolean>

    @Query("""SELECT * FROM favorite WHERE departure_code = :departureCode AND destination_code = :destinationCode LIMIT 1""")
    suspend fun getFavorite(departureCode: String, destinationCode: String): Favorite?
}