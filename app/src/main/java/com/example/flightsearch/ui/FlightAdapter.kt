package com.example.flightsearch.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.FlightSearch.R
import com.example.FlightSearch.databinding.ItemFlightBinding
import com.example.flightsearch.data.Flight

class FlightAdapter(
    private val onFavoriteClick: (Flight) -> Unit
) : ListAdapter<Flight, FlightAdapter.FlightViewHolder>(FlightDiffCallback()) {

    inner class FlightViewHolder(
        private val binding: ItemFlightBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(flight: Flight) {
            with(binding) {
                departureCode.text = flight.departureAirport.iataCode
                departureName.text = flight.departureAirport.name
                destinationCode.text = flight.destinationAirport.iataCode
                destinationName.text = flight.destinationAirport.name

                favoriteButton.apply {
                    setIconResource(
                        if (flight.isFavorite) R.drawable.ic_favorite
                        else R.drawable.ic_favorite_border
                    )

                    contentDescription = itemView.context.getString(
                        if (flight.isFavorite) R.string.remove_from_favorites
                        else R.string.add_to_favorites
                    )

                    setOnClickListener {
                        onFavoriteClick(flight)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FlightViewHolder {
        return FlightViewHolder(
            ItemFlightBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: FlightViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class FlightDiffCallback : DiffUtil.ItemCallback<Flight>() {
        override fun areItemsTheSame(oldItem: Flight, newItem: Flight): Boolean =
            oldItem.departureAirport.id == newItem.departureAirport.id &&
                    oldItem.destinationAirport.id == newItem.destinationAirport.id

        override fun areContentsTheSame(oldItem: Flight, newItem: Flight): Boolean =
            oldItem == newItem
    }
}