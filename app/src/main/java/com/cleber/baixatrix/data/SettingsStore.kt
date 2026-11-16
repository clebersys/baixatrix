package com.cleber.baixatrix.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "baixatrix_settings")

/** Preferências do app: endereço do servidor de extração opcional. */
class SettingsStore(private val context: Context) {

    data class Settings(
        val resolverUrl: String = ""
    )

    val settings: Flow<Settings> = context.dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            Settings(
                resolverUrl = prefs[Keys.RESOLVER_URL].orEmpty()
            )
        }

    suspend fun current(): Settings = settings.first()

    suspend fun setResolverUrl(value: String) {
        context.dataStore.edit { it[Keys.RESOLVER_URL] = value.trim() }
    }

    private object Keys {
        val RESOLVER_URL = stringPreferencesKey("resolver_url")
    }
}
