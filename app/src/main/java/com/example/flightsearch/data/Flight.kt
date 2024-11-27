package com.example.flightsearch.data

data class Flight(
    val departureAirport: Airport,
    val destinationAirport: Airport,
    var isFavorite: Boolean = false
)