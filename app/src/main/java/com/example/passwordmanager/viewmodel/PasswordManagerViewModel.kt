package com.example.passwordmanager.viewmodel

import androidx.lifecycle.ViewModel
import com.example.passwordmanager.model.PasswordCategory
import com.example.passwordmanager.model.PasswordFormState
import com.example.passwordmanager.model.PasswordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class PasswordManagerViewModel : ViewModel() {
    private val repository = PasswordRepository()

    private val _uiState = MutableStateFlow(
        PasswordManagerUiState(passwords = repository.getPasswords())
    )
    val uiState: StateFlow<PasswordManagerUiState> = _uiState

    fun onSearchChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onCategorySelected(category: PasswordCategory?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun toggleFormVisibility() {
        _uiState.update {
            it.copy(
                isFormVisible = !it.isFormVisible,
                errorMessage = null
            )
        }
    }

    fun updateServiceName(value: String) {
        updateForm { it.copy(serviceName = value) }
    }

    fun updateUsername(value: String) {
        updateForm { it.copy(username = value) }
    }

    fun updatePassword(value: String) {
        updateForm { it.copy(password = value) }
    }

    fun updateCategory(category: PasswordCategory) {
        updateForm { it.copy(category = category) }
    }

    fun updateNote(value: String) {
        updateForm { it.copy(note = value) }
    }

    fun generatePassword() {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
        val symbols = "!@#$%&*?"
        val generated = buildString {
            repeat(10) { append(alphabet.random()) }
            repeat(4) { append(symbols.random()) }
        }.toList().shuffled().joinToString("")

        updateForm { it.copy(password = generated) }
    }

    fun addPassword() {
        val form = _uiState.value.formState
        val validationError = validate(form)

        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        val updatedPasswords = repository.addPassword(form)
        _uiState.update {
            it.copy(
                passwords = updatedPasswords,
                formState = PasswordFormState(),
                isFormVisible = false,
                errorMessage = null
            )
        }
    }

    fun deletePassword(id: String) {
        _uiState.update {
            it.copy(
                passwords = repository.deletePassword(id),
                visiblePasswordIds = it.visiblePasswordIds - id
            )
        }
    }

    fun togglePasswordVisibility(id: String) {
        _uiState.update {
            val updatedIds = if (id in it.visiblePasswordIds) {
                it.visiblePasswordIds - id
            } else {
                it.visiblePasswordIds + id
            }
            it.copy(visiblePasswordIds = updatedIds)
        }
    }

    private fun updateForm(reducer: (PasswordFormState) -> PasswordFormState) {
        _uiState.update {
            it.copy(
                formState = reducer(it.formState),
                errorMessage = null
            )
        }
    }

    private fun validate(form: PasswordFormState): String? {
        return when {
            form.serviceName.isBlank() -> "Enter service name"
            form.username.isBlank() -> "Enter username or email"
            form.password.length < 6 -> "Password must contain at least 6 characters"
            else -> null
        }
    }
}
