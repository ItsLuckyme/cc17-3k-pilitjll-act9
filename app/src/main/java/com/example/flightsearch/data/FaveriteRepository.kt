package com.example.flightsearch.data

import kotlinx.coroutines.flow.Flow

class FavoriteRepository(private val favoriteDao: FavoriteDao) {
    fun getAllFavorites(): Flow<List<Favorite>> {
        return favoriteDao.getAllFavorites()
    }

    suspend fun addFavorite(favorite: Favorite) {
        favoriteDao.insertFavorite(favorite)
    }

    suspend fun removeFavorite(favorite: Favorite) {
        favoriteDao.deleteFavorite(favorite)
    }

    fun isRouteFavorite(departureCode: String, destinationCode: String): Flow<Boolean> {
        return favoriteDao.isFavorite(departureCode, destinationCode)
    }

    suspend fun getFavorite(departureCode: String, destinationCode: String): Favorite? {
        return favoriteDao.getFavorite(departureCode, destinationCode)
    }
}