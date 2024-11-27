package com.example.flightsearch

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.FlightSearch.R
import com.example.flightsearch.data.Airport
import com.example.flightsearch.data.AirportRepository
import com.example.flightsearch.data.Favorite
import com.example.flightsearch.data.FavoriteRepository
import com.example.flightsearch.data.Flight
import com.example.flightsearch.data.FlightDatabase
import com.example.flightsearch.data.PreferencesManager
import com.example.flightsearch.ui.AirportSuggestionAdapter
import com.example.flightsearch.ui.FavoriteAdapter
import com.example.flightsearch.ui.FlightAdapter
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var searchEditText: TextInputEditText
    private lateinit var airportAdapter: AirportSuggestionAdapter
    private lateinit var airportRepository: AirportRepository
    private lateinit var flightAdapter: FlightAdapter
    private lateinit var favoriteRepository: FavoriteRepository
    private val searchJob = Job()
    private val searchScope = CoroutineScope(Dispatchers.Main + searchJob)
    private val flightJob = Job()
    private val flightScope = CoroutineScope(Dispatchers.Main + flightJob)
    private lateinit var favoriteAdapter: FavoriteAdapter
    private val favoriteJob = Job()
    private val favoriteScope = CoroutineScope(Dispatchers.Main + favoriteJob)
    private var currentScrollPosition = 0
    private enum class DisplayState {
        FAVORITES,
        SEARCH_RESULTS,
        FLIGHTS
    }
    private var currentDisplayState = DisplayState.FAVORITES

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupDependencies()
        setupRecyclerView()
        setupSearchView()
        setupFlightRecyclerView()
        setupFavorites()
        restoreAppState()
        setupBackNavigation()
    }

    private fun setupDependencies() {
        try {
            Log.d("MainActivity", "Initializing dependencies")
            preferencesManager = PreferencesManager(this)
            val database = FlightDatabase.getDatabase(this)
            Log.d("MainActivity", "Database initialized")

            airportRepository = AirportRepository(database.airportDao())
            favoriteRepository = FavoriteRepository(database.favoriteDao())
            Log.d("MainActivity", "Repositories initialized")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error initializing dependencies", e)
            Toast.makeText(
                this,
                "Error initializing app: ${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.search_results)
        airportAdapter = AirportSuggestionAdapter { airport ->
            handleAirportSelection(airport)
        }
        recyclerView.apply {
            adapter = airportAdapter as RecyclerView.Adapter<RecyclerView.ViewHolder>
            layoutManager = LinearLayoutManager(this@MainActivity)
            addItemDecoration(
                DividerItemDecoration(this@MainActivity, DividerItemDecoration.VERTICAL)
            )
        }
    }

    private fun setupSearchView() {
        searchEditText = findViewById(R.id.airport_search)

        lifecycleScope.launch {
            preferencesManager.searchQuery.collect { savedQuery ->
                if (searchEditText.text.toString() != savedQuery) {
                    searchEditText.setText(savedQuery)
                }
            }
        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchAirports(s?.toString() ?: "")
            }
        })
    }

    private fun searchAirports(query: String) {
        if (query.isBlank()) {
            showFavorites()
            return
        }

        updateDisplayState(DisplayState.SEARCH_RESULTS)
        searchScope.cancel()

        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "Searching for query: $query")
                favoriteAdapter.submitList(emptyList())

                airportRepository.searchAirports(query).collect { airports ->
                    Log.d("MainActivity", "Found ${airports.size} airports")
                    if (airports.isEmpty()) {
                        Log.d("MainActivity", "No airports found")
                        showEmptyState()
                    } else {
                        hideEmptyState()
                        findViewById<RecyclerView>(R.id.search_results).adapter =
                            airportAdapter as RecyclerView.Adapter<RecyclerView.ViewHolder>
                        airportAdapter.submitList(airports)
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error searching airports", e)
            }
        }
    }

    private fun showEmptyState() {
        findViewById<TextView>(R.id.empty_state).apply {
            visibility = View.VISIBLE
            text = getString(R.string.no_results)
        }
        findViewById<RecyclerView>(R.id.search_results).visibility = View.GONE
    }

    private fun hideEmptyState() {
        findViewById<TextView>(R.id.empty_state).visibility = View.GONE
        findViewById<RecyclerView>(R.id.search_results).visibility = View.VISIBLE
    }

    private fun handleAirportSelection(selectedAirport: Airport) {
        updateDisplayState(DisplayState.FLIGHTS)
        searchEditText.clearFocus()
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(searchEditText.windowToken, 0)

        supportActionBar?.title = getString(R.string.flights_from, selectedAirport.iataCode)

        airportAdapter.submitList(emptyList())
        favoriteAdapter.submitList(emptyList())

        val recyclerView = findViewById<RecyclerView>(R.id.search_results)
        recyclerView.adapter = flightAdapter

        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "Loading flights from ${selectedAirport.iataCode}")
                airportRepository.getDestinationAirports(selectedAirport.iataCode)
                    .collect { destinations ->
                        Log.d("MainActivity", "Found ${destinations.size} destinations")
                        val flights = destinations.map { destination ->
                            Flight(
                                departureAirport = selectedAirport,
                                destinationAirport = destination,
                                isFavorite = false
                            )
                        }
                        flightAdapter.submitList(flights)

                        flights.forEach { flight ->
                            favoriteRepository.isRouteFavorite(
                                flight.departureAirport.iataCode,
                                flight.destinationAirport.iataCode
                            ).collect { isFavorite ->
                                flight.isFavorite = isFavorite
                                val position = flightAdapter.currentList.indexOf(flight)
                                if (position != -1) {
                                    flightAdapter.notifyItemChanged(position)
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error loading flights", e)
                Toast.makeText(
                    this@MainActivity,
                    "Error loading flights: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showFavorites() {
        updateDisplayState(DisplayState.FAVORITES)
        airportAdapter.submitList(emptyList())
        flightAdapter.submitList(emptyList())

        findViewById<RecyclerView>(R.id.search_results).adapter =
            favoriteAdapter as RecyclerView.Adapter<RecyclerView.ViewHolder>

        favoriteScope.launch {
            favoriteRepository.getAllFavorites().collect { favorites ->
                if (favorites.isEmpty()) {
                    showEmptyFavorites()
                } else {
                    hideEmptyState()
                    favorites.forEach { favorite ->
                        favorite.departureAirport = airportRepository.getAirportByCode(favorite.departureCode)
                        favorite.destinationAirport = airportRepository.getAirportByCode(favorite.destinationCode)
                    }
                    favoriteAdapter.submitList(favorites)
                }
            }
        }
    }

    private fun showEmptyFavorites() {
        findViewById<TextView>(R.id.empty_state).apply {
            visibility = View.VISIBLE
            text = getString(R.string.no_favorites)
        }
    }

    private fun handleFavoriteDelete(favorite: Favorite) {
        favoriteScope.launch {
            favoriteRepository.removeFavorite(favorite)
            val remainingFavorites = favoriteRepository.getAllFavorites().first()
            if (remainingFavorites.isEmpty()) {
                withContext(Dispatchers.Main) {
                    updateEmptyState(true)
                }
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        findViewById<TextView>(R.id.empty_state).visibility = if (isEmpty) View.VISIBLE else View.GONE
        findViewById<RecyclerView>(R.id.search_results).visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun showFlightResults(departure: Airport, destinations: List<Airport>) {
        flightScope.launch {
            val flights = destinations.map { destination ->
                Flight(departure, destination).also { flight ->
                    favoriteRepository.isRouteFavorite(
                        flight.departureAirport.iataCode,
                        flight.destinationAirport.iataCode
                    ).collect { isFavorite ->
                        flight.isFavorite = isFavorite
                        flightAdapter.notifyItemChanged(flightAdapter.currentList.indexOf(flight))
                    }
                }
            }
            flightAdapter.submitList(flights)
        }
    }

    private fun setupFlightRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.search_results)
        flightAdapter = FlightAdapter { flight ->
            handleFavoriteClick(flight)
        }
        recyclerView.apply {
            adapter = flightAdapter as RecyclerView.Adapter<RecyclerView.ViewHolder>
            layoutManager = LinearLayoutManager(this@MainActivity)
            addItemDecoration(
                DividerItemDecoration(this@MainActivity, DividerItemDecoration.VERTICAL)
            )
        }
    }

    private fun handleFavoriteClick(flight: Flight) {
        lifecycleScope.launch {
            val isFavorite = favoriteRepository.isRouteFavorite(
                flight.departureAirport.iataCode,
                flight.destinationAirport.iataCode
            ).first()

            if (isFavorite) {
                val favorite = favoriteRepository.getFavorite(
                    flight.departureAirport.iataCode,
                    flight.destinationAirport.iataCode
                )
                favorite?.let {
                    favoriteRepository.removeFavorite(it)
                    flight.isFavorite = false
                    val position = flightAdapter.currentList.indexOf(flight)
                    if (position != -1) {
                        flightAdapter.notifyItemChanged(position)
                    }
                }
            } else {
                val newFavorite = Favorite(
                    departureCode = flight.departureAirport.iataCode,
                    destinationCode = flight.destinationAirport.iataCode
                )
                favoriteRepository.addFavorite(newFavorite)
                flight.isFavorite = true
                val position = flightAdapter.currentList.indexOf(flight)
                if (position != -1) {
                    flightAdapter.notifyItemChanged(position)
                }
            }
        }
    }

    private fun setupFavorites() {
        favoriteAdapter = FavoriteAdapter { favorite ->
            handleFavoriteDelete(favorite)
        }

        if (searchEditText.text.isNullOrEmpty()) {
            showFavorites()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        searchJob.cancel()
        flightJob.cancel()
        favoriteJob.cancel()
    }

    override fun onPause() {
        super.onPause()
        saveAppState()
    }

    private fun saveAppState() {
        lifecycleScope.launch {
            val recyclerView = findViewById<RecyclerView>(R.id.search_results)
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            currentScrollPosition = layoutManager.findFirstVisibleItemPosition()

            preferencesManager.apply {
                saveSearchQuery(searchEditText.text?.toString() ?: "")
                saveScrollPosition(currentScrollPosition)
            }
        }
    }

    private fun restoreAppState() {
        lifecycleScope.launch {
            preferencesManager.scrollPosition.collect { position ->
                val recyclerView = findViewById<RecyclerView>(R.id.search_results)
                (recyclerView.layoutManager as LinearLayoutManager)
                    .scrollToPosition(position)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("SCROLL_POSITION", currentScrollPosition)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        currentScrollPosition = savedInstanceState.getInt("SCROLL_POSITION", 0)
        val recyclerView = findViewById<RecyclerView>(R.id.search_results)
        recyclerView.layoutManager?.scrollToPosition(currentScrollPosition)
    }

    private fun updateDisplayState(newState: DisplayState) {
        if (currentDisplayState != newState) {
            currentDisplayState = newState
            airportAdapter.submitList(emptyList())
            favoriteAdapter.submitList(emptyList())
            flightAdapter.submitList(emptyList())
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this) {
            when {
                !searchEditText.text.isNullOrEmpty() -> {
                    searchEditText.setText("")
                    showFavorites()
                }
                currentDisplayState == DisplayState.FLIGHTS -> {
                    showFavorites()
                }
                else -> {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }
    }
}