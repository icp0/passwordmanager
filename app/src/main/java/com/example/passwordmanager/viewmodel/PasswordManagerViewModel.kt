package com.example.passwordmanager.viewmodel

import androidx.lifecycle.ViewModel
import com.example.passwordmanager.model.AuthenticationState
import com.example.passwordmanager.model.AutofillManager
import com.example.passwordmanager.model.AutofillSuggestion
import com.example.passwordmanager.model.BiometricAccessManager
import com.example.passwordmanager.model.BiometricAvailability
import com.example.passwordmanager.model.PasswordCategory
import com.example.passwordmanager.model.PasswordEntry
import com.example.passwordmanager.model.PasswordFormState
import com.example.passwordmanager.model.PasswordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class PasswordManagerViewModel : ViewModel() {
    private val repository = PasswordRepository()
    private val biometricAccessManager = BiometricAccessManager()
    private val autofillManager = AutofillManager()

    private val _uiState = MutableStateFlow(
        PasswordManagerUiState(
            passwords = repository.getPasswords(),
            biometricPromptData = biometricAccessManager.createPromptData()
        )
    )
    val uiState: StateFlow<PasswordManagerUiState> = _uiState

    fun onBiometricAvailabilityChecked(availability: BiometricAvailability) {
        val authenticationState = if (availability == BiometricAvailability.AVAILABLE) {
            AuthenticationState.LOCKED
        } else {
            AuthenticationState.UNAVAILABLE
        }
        val message = if (availability == BiometricAvailability.AVAILABLE) {
            "Биометрическая аутентификация доступна"
        } else {
            "Биометрическая аутентификация недоступна: ${availability.toRussianText()}"
        }

        _uiState.update {
            it.copy(
                biometricAvailability = availability,
                authenticationState = authenticationState,
                authenticationMessage = message,
                isBiometricPromptRequested = false
            )
        }
    }

    fun requestVaultUnlock() {
        _uiState.update {
            if (it.authenticationState == AuthenticationState.UNLOCKED) {
                it.copy(authenticationMessage = "Хранилище уже разблокировано")
            } else if (it.biometricAvailability == BiometricAvailability.AVAILABLE) {
                it.copy(
                    isBiometricPromptRequested = true,
                    authenticationMessage = "Подтвердите биометрию для разблокировки"
                )
            } else {
                it.copy(authenticationMessage = "Биометрическая аутентификация недоступна")
            }
        }
    }

    fun onBiometricAuthenticationSucceeded() {
        _uiState.update {
            it.copy(
                authenticationState = AuthenticationState.UNLOCKED,
                isBiometricPromptRequested = false,
                authenticationMessage = "Хранилище разблокировано",
                errorMessage = null
            )
        }
    }

    fun onBiometricAuthenticationFailed() {
        _uiState.update {
            it.copy(authenticationMessage = "Биометрическая проверка не прошла, попробуйте ещё раз")
        }
    }

    fun onBiometricAuthenticationError(message: String) {
        _uiState.update {
            it.copy(
                isBiometricPromptRequested = false,
                authenticationMessage = message
            )
        }
    }

    fun lockVault() {
        _uiState.update {
            if (it.biometricAvailability == BiometricAvailability.AVAILABLE) {
                it.copy(
                    authenticationState = AuthenticationState.LOCKED,
                    visiblePasswordIds = emptySet(),
                    autofillProfile = it.autofillProfile.copy(selectedSuggestion = null),
                    authenticationMessage = "Хранилище заблокировано"
                )
            } else {
                it
            }
        }
    }

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
                autofillSuggestions = buildAutofillSuggestions(
                    requestedService = it.autofillProfile.requestedService,
                    passwords = updatedPasswords
                ),
                errorMessage = null
            )
        }
    }

    fun deletePassword(id: String) {
        val updatedPasswords = repository.deletePassword(id)
        _uiState.update {
            it.copy(
                passwords = updatedPasswords,
                visiblePasswordIds = it.visiblePasswordIds - id,
                autofillSuggestions = buildAutofillSuggestions(
                    requestedService = it.autofillProfile.requestedService,
                    passwords = updatedPasswords
                ),
                autofillProfile = if (it.autofillProfile.selectedSuggestion?.entryId == id) {
                    it.autofillProfile.copy(selectedSuggestion = null)
                } else {
                    it.autofillProfile
                }
            )
        }
    }

    fun togglePasswordVisibility(id: String) {
        _uiState.update {
            if (it.authenticationState == AuthenticationState.LOCKED) {
                return@update it.copy(
                    isBiometricPromptRequested = it.biometricAvailability == BiometricAvailability.AVAILABLE,
                    authenticationMessage = "Разблокируйте хранилище, чтобы показать пароль"
                )
            }

            val updatedIds = if (id in it.visiblePasswordIds) {
                it.visiblePasswordIds - id
            } else {
                it.visiblePasswordIds + id
            }
            it.copy(visiblePasswordIds = updatedIds)
        }
    }

    fun updateAutofillService(value: String) {
        _uiState.update {
            it.copy(
                autofillProfile = it.autofillProfile.copy(
                    requestedService = value,
                    selectedSuggestion = null
                ),
                autofillSuggestions = buildAutofillSuggestions(value, it.passwords)
            )
        }
    }

    fun selectAutofillSuggestion(suggestion: AutofillSuggestion) {
        _uiState.update {
            if (it.authenticationState == AuthenticationState.LOCKED) {
                return@update it.copy(
                    isBiometricPromptRequested = it.biometricAvailability == BiometricAvailability.AVAILABLE,
                    authenticationMessage = "Разблокируйте хранилище, чтобы использовать автозаполнение"
                )
            }

            it.copy(
                autofillProfile = it.autofillProfile.copy(selectedSuggestion = suggestion),
                authenticationMessage = "Автозаполнение подготовлено для ${suggestion.serviceName}"
            )
        }
    }

    fun clearAutofillSelection() {
        _uiState.update {
            it.copy(
                autofillProfile = it.autofillProfile.copy(selectedSuggestion = null)
            )
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
            form.serviceName.isBlank() -> "Введите название сервиса"
            form.username.isBlank() -> "Введите логин или email"
            form.password.length < 6 -> "Пароль должен содержать минимум 6 символов"
            else -> null
        }
    }

    private fun buildAutofillSuggestions(
        requestedService: String,
        passwords: List<PasswordEntry>
    ): List<AutofillSuggestion> {
        return autofillManager.findSuggestions(
            requestedService = requestedService,
            passwords = passwords
        )
    }

    private fun BiometricAvailability.toRussianText(): String {
        return when (this) {
            BiometricAvailability.AVAILABLE -> "доступна"
            BiometricAvailability.NO_HARDWARE -> "нет биометрического датчика"
            BiometricAvailability.HARDWARE_UNAVAILABLE -> "датчик временно недоступен"
            BiometricAvailability.NOT_ENROLLED -> "биометрия не настроена"
            BiometricAvailability.SECURITY_UPDATE_REQUIRED -> "требуется обновление безопасности"
            BiometricAvailability.UNSUPPORTED -> "не поддерживается устройством"
            BiometricAvailability.UNKNOWN -> "неизвестная ошибка"
        }
    }
}
