package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.MessageDigest
import java.security.SecureRandom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kimagen_prefs")

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val VAULT_PIN_HASH = stringPreferencesKey("vault_pin_hash")
        val VAULT_PIN_SALT = stringPreferencesKey("vault_pin_salt")
        val VIEW_DENSITY = stringPreferencesKey("view_density")
        val LAST_BACKUP_MILLIS = longPreferencesKey("last_backup_millis")
    }

    val hasPinConfigured: Flow<Boolean> = context.dataStore.data.map { prefs ->
        !prefs[Keys.VAULT_PIN_HASH].isNullOrBlank()
    }

    val viewDensity: Flow<ViewDensity> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.VIEW_DENSITY]
        runCatching { if (raw != null) ViewDensity.valueOf(raw) else ViewDensity.STANDARD }
            .getOrDefault(ViewDensity.STANDARD)
    }

    val lastBackupMillis: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_BACKUP_MILLIS]
    }

    suspend fun setViewDensity(density: ViewDensity) {
        context.dataStore.edit { prefs ->
            prefs[Keys.VIEW_DENSITY] = density.name
        }
    }

    suspend fun setLastBackupMillis(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_BACKUP_MILLIS] = timestamp
        }
    }

    suspend fun saveVaultPin(pin: String) {
        val saltBytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val saltHex = saltBytes.joinToString("") { "%02x".format(it) }
        val hashHex = sha256Hex(saltHex + pin)
        context.dataStore.edit { prefs ->
            prefs[Keys.VAULT_PIN_SALT] = saltHex
            prefs[Keys.VAULT_PIN_HASH] = hashHex
        }
    }

    suspend fun verifyVaultPin(pin: String): Boolean {
        val prefs = context.dataStore.data.first()
        val storedHash = prefs[Keys.VAULT_PIN_HASH] ?: return false
        val storedSalt = prefs[Keys.VAULT_PIN_SALT] ?: return false
        val candidateHash = sha256Hex(storedSalt + pin)
        return MessageDigest.isEqual(storedHash.toByteArray(), candidateHash.toByteArray())
    }

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
