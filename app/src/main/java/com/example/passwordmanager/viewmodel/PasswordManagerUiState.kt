package com.example.passwordmanager.viewmodel

import com.example.passwordmanager.model.AuthenticationState
import com.example.passwordmanager.model.AutofillProfile
import com.example.passwordmanager.model.AutofillSuggestion
import com.example.passwordmanager.model.BiometricAvailability
import com.example.passwordmanager.model.BiometricPromptData
import com.example.passwordmanager.model.PasswordCategory
import com.example.passwordmanager.model.PasswordEntry
import com.example.passwordmanager.model.PasswordFormState

data class PasswordManagerUiState(
    val passwords: List<PasswordEntry> = emptyList(),
    val authenticationState: AuthenticationState = AuthenticationState.LOCKED,
    val biometricAvailability: BiometricAvailability = BiometricAvailability.UNKNOWN,
    val biometricPromptData: BiometricPromptData = BiometricPromptData(),
    val isBiometricPromptRequested: Boolean = false,
    val authenticationMessage: String = "Разблокируйте хранилище, чтобы смотреть пароли и использовать автозаполнение",
    val searchQuery: String = "",
    val selectedCategory: PasswordCategory? = null,
    val formState: PasswordFormState = PasswordFormState(),
    val isFormVisible: Boolean = false,
    val visiblePasswordIds: Set<String> = emptySet(),
    val autofillProfile: AutofillProfile = AutofillProfile(),
    val autofillSuggestions: List<AutofillSuggestion> = emptyList(),
    val errorMessage: String? = null
) {
    val filteredPasswords: List<PasswordEntry>
        get() = passwords.filter { entry ->
            val matchesCategory = selectedCategory == null || entry.category == selectedCategory
            val matchesSearch = searchQuery.isBlank() ||
                entry.serviceName.contains(searchQuery, ignoreCase = true) ||
                entry.username.contains(searchQuery, ignoreCase = true) ||
                entry.note.contains(searchQuery, ignoreCase = true)

            matchesCategory && matchesSearch
        }
}
