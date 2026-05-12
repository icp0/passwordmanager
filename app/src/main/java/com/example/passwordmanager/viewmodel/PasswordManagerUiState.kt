package com.example.passwordmanager.viewmodel

import com.example.passwordmanager.model.PasswordCategory
import com.example.passwordmanager.model.PasswordEntry
import com.example.passwordmanager.model.PasswordFormState

data class PasswordManagerUiState(
    val passwords: List<PasswordEntry> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: PasswordCategory? = null,
    val formState: PasswordFormState = PasswordFormState(),
    val isFormVisible: Boolean = false,
    val visiblePasswordIds: Set<String> = emptySet(),
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
