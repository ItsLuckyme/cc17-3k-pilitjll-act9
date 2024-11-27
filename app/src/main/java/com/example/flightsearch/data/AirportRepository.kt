package com.example.flightsearch.data

import kotlinx.coroutines.flow.Flow

class AirportRepository(private val airportDao: AirportDao) {
    fun searchAirports(query: String): Flow<List<Airport>> {
        return airportDao.getAirportsBySearchQuery("%${query}%")
    }

    fun getDestinationAirports(departureCode: String): Flow<List<Airport>> {
        return airportDao.getAllAirportsExcept(departureCode)
    }

    suspend fun getAirportByCode(code: String): Airport? {
        return airportDao.getAirportByCode(code)
    }
}