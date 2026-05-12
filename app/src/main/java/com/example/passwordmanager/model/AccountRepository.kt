package com.example.passwordmanager.model

import android.content.Context
import java.security.MessageDigest

class AccountRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun hasAccount(): Boolean {
        return preferences.contains(KEY_PIN_HASH)
    }

    fun savePin(pin: String) {
        preferences.edit()
            .putString(KEY_PIN_HASH, pin.sha256())
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        return preferences.getString(KEY_PIN_HASH, null) == pin.sha256()
    }

    private fun String.sha256(): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val PREFERENCES_NAME = "password_manager_account"
        const val KEY_PIN_HASH = "pin_hash"
    }
}
