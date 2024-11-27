package com.example.flightsearch.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Ignore

@Entity(tableName = "favorite")
data class Favorite(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    @ColumnInfo(name = "departure_code")
    var departureCode: String,
    @ColumnInfo(name = "destination_code")
    var destinationCode: String
) {
    constructor(
        id: Int = 0,
        departureCode: String,
        destinationCode: String,
        departureAirport: Airport?,
        destinationAirport: Airport?
    ) : this(id, departureCode, destinationCode) {
        this.departureAirport = departureAirport
        this.destinationAirport = destinationAirport
    }

    @Ignore
    var departureAirport: Airport? = null

    @Ignore
    var destinationAirport: Airport? = null
}