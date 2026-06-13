package com.cleanify.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * A type-safe, generic wrapper around a single DataStore preference.
 *
 * Eliminates the repeated flow+setter boilerplate in PreferencesRepository.
 * Before: ~10 lines per preference (flow + setter).
 * After: ~1 line per preference.
 *
 * @param T The type of the preference value.
 */
class Preference<T>(
    val dataStore: DataStore<Preferences>,
    private val key: Preferences.Key<T>,
    private val default: T
) {
    /** A Flow that emits the current value of this preference, defaulting if unset. */
    val flow: Flow<T> = dataStore.data.map { prefs -> prefs[key] ?: default }

    /** Sets the preference value. Suspends while the DataStore write completes. */
    suspend fun set(value: T) {
        dataStore.edit { prefs -> prefs[key] = value }
    }

    /** Convenience: read the current value (suspending). */
    suspend fun get(): T = flow.first()
}

/**
 * A type-safe wrapper for enum preferences stored as their [Enum.name] string.
 *
 * @param E The enum type.
 */
class EnumPreference<E : Enum<E>>(
    private val dataStore: DataStore<Preferences>,
    private val key: Preferences.Key<String>,
    private val default: E,
    private val clazz: Class<E>
) {
    /** A Flow that emits the current enum value, falling back to [default] on parse error. */
    val flow: Flow<E> = dataStore.data.map { prefs ->
        prefs[key]?.let { raw ->
            clazz.enumConstants?.firstOrNull { it.name == raw }
        } ?: default
    }

    /** Sets the preference value by storing its [Enum.name]. */
    suspend fun set(value: E) {
        dataStore.edit { prefs -> prefs[key] = value.name }
    }

    /** Convenience: read the current value (suspending). */
    suspend fun get(): E = flow.first()
}

/**
 * A type-safe wrapper for Set<String> preferences using [stringSetPreferencesKey].
 * Replaces the fragile comma-separated string encoding.
 */
class StringSetPreference(
    private val dataStore: DataStore<Preferences>,
    private val key: Preferences.Key<Set<String>>,
    private val default: Set<String> = emptySet()
) {
    /** A Flow that emits the current set value. */
    val flow: Flow<Set<String>> = dataStore.data.map { prefs -> prefs[key] ?: default }

    /** Adds one or more values to the set. */
    suspend fun add(vararg values: String) {
        dataStore.edit { prefs ->
            val current = prefs[key] ?: emptySet()
            prefs[key] = current + values
        }
    }

    /** Removes one or more values from the set. */
    suspend fun remove(vararg values: String) {
        dataStore.edit { prefs ->
            val current = prefs[key] ?: emptySet()
            prefs[key] = current - values
        }
    }

    /** Replaces the entire set with the given values. */
    suspend fun set(values: Set<String>) {
        dataStore.edit { prefs -> prefs[key] = values }
    }

    /** Clears all values. */
    suspend fun clear() {
        dataStore.edit { prefs -> prefs.remove(key) }
    }
}
