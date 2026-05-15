package com.example.passwordmanager.viewmodel

import com.example.passwordmanager.model.AuthenticationState
import com.example.passwordmanager.model.AutofillProfile
import com.example.passwordmanager.model.AutofillSuggestion
import com.example.passwordmanager.model.BiometricAvailability
import com.example.passwordmanager.model.BiometricPromptData
import com.example.passwordmanager.model.PasswordCategory
import com.example.passwordmanager.model.PasswordEntry
import com.example.passwordmanager.model.PasswordFormState

enum class AppTab(val title: String) {
    PASSWORDS("Пароли"),
    ADD_PASSWORD("Добавить"),
    BACKUP("Резерв")
}

data class PasswordManagerUiState(
    val passwords: List<PasswordEntry> = emptyList(),
    val hasAccount: Boolean = false,
    val registrationPin: String = "",
    val registrationPinRepeat: String = "",
    val loginPin: String = "",
    val authenticationState: AuthenticationState = AuthenticationState.LOCKED,
    val biometricAvailability: BiometricAvailability = BiometricAvailability.UNKNOWN,
    val biometricPromptData: BiometricPromptData = BiometricPromptData(),
    val isBiometricPromptRequested: Boolean = false,
    val authenticationMessage: String = "Создайте PIN-код для защиты хранилища",
    val selectedTab: AppTab = AppTab.PASSWORDS,
    val searchQuery: String = "",
    val selectedCategory: PasswordCategory? = null,
    val formState: PasswordFormState = PasswordFormState(),
    val visiblePasswordIds: Set<String> = emptySet(),
    val autofillProfile: AutofillProfile = AutofillProfile(),
    val autofillSuggestions: List<AutofillSuggestion> = emptyList(),
    val exportedBackupText: String = "",
    val importBackupText: String = "",
    val backupMessage: String? = null,
    val errorMessage: String? = null
) {
    val isUnlocked: Boolean
        get() = authenticationState == AuthenticationState.UNLOCKED

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
