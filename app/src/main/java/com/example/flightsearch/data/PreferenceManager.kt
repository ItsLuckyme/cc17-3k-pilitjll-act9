package com.example.flightsearch.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {

    companion object {
        private val SEARCH_QUERY = stringPreferencesKey("search_query")
        private val SCROLL_POSITION = intPreferencesKey("scroll_position")
    }

    val searchQuery: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SEARCH_QUERY] ?: ""
    }

    val scrollPosition: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[SCROLL_POSITION] ?: 0
    }

    suspend fun saveSearchQuery(query: String) {
        context.dataStore.edit { preferences ->
            preferences[SEARCH_QUERY] = query
        }
    }

    suspend fun saveScrollPosition(position: Int) {
        context.dataStore.edit { preferences ->
            preferences[SCROLL_POSITION] = position
        }
    }
}