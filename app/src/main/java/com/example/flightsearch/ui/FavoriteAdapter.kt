package com.example.flightsearch.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.FlightSearch.databinding.ItemFavoriteRouteBinding
import com.example.flightsearch.data.Favorite

class FavoriteAdapter(
    private val onDeleteClick: (Favorite) -> Unit
) : ListAdapter<Favorite, FavoriteAdapter.FavoriteViewHolder>(FavoriteDiffCallback()) {

    inner class FavoriteViewHolder(
        private val binding: ItemFavoriteRouteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(favorite: Favorite) {
            binding.apply {
                departureCode.text = favorite.departureCode
                departureName.text = favorite.departureAirport?.name ?: ""
                destinationCode.text = favorite.destinationCode
                destinationName.text = favorite.destinationAirport?.name ?: ""
                deleteButton.setOnClickListener { onDeleteClick(favorite) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        return FavoriteViewHolder(
            ItemFavoriteRouteBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class FavoriteDiffCallback : DiffUtil.ItemCallback<Favorite>() {
        override fun areItemsTheSame(oldItem: Favorite, newItem: Favorite) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Favorite, newItem: Favorite) =
            oldItem == newItem
    }
}