package com.example.passwordmanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.passwordmanager.model.AuthenticationState
import com.example.passwordmanager.model.AccountRepository
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

class PasswordManagerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PasswordRepository()
    private val accountRepository = AccountRepository(application)
    private val biometricAccessManager = BiometricAccessManager()
    private val autofillManager = AutofillManager()

    private val _uiState = MutableStateFlow(
        PasswordManagerUiState(
            passwords = repository.getPasswords(),
            hasAccount = accountRepository.hasAccount(),
            authenticationState = if (accountRepository.hasAccount()) {
                AuthenticationState.LOCKED
            } else {
                AuthenticationState.LOCKED
            },
            authenticationMessage = if (accountRepository.hasAccount()) {
                "Войдите по PIN-коду или биометрии"
            } else {
                "Создайте PIN-код для защиты хранилища"
            },
            biometricPromptData = biometricAccessManager.createPromptData()
        )
    )
    val uiState: StateFlow<PasswordManagerUiState> = _uiState

    fun onBiometricAvailabilityChecked(availability: BiometricAvailability) {
        _uiState.update {
            it.copy(
                biometricAvailability = availability,
                authenticationMessage = if (it.hasAccount) {
                    if (availability == BiometricAvailability.AVAILABLE) {
                        "Войдите по PIN-коду или биометрии"
                    } else {
                        "Войдите по PIN-коду"
                    }
                } else {
                    "Создайте PIN-код для защиты хранилища"
                }
            )
        }
    }

    fun updateRegistrationPin(value: String) {
        _uiState.update { it.copy(registrationPin = value.onlyDigits(6), errorMessage = null) }
    }

    fun updateRegistrationPinRepeat(value: String) {
        _uiState.update { it.copy(registrationPinRepeat = value.onlyDigits(6), errorMessage = null) }
    }

    fun registerAccount() {
        val state = _uiState.value
        val pin = state.registrationPin
        val repeatedPin = state.registrationPinRepeat

        when {
            pin.length < MIN_PIN_LENGTH -> {
                _uiState.update { it.copy(errorMessage = "PIN-код должен содержать минимум 4 цифры") }
            }
            pin != repeatedPin -> {
                _uiState.update { it.copy(errorMessage = "PIN-коды не совпадают") }
            }
            else -> {
                accountRepository.savePin(pin)
                _uiState.update {
                    it.copy(
                        hasAccount = true,
                        registrationPin = "",
                        registrationPinRepeat = "",
                        loginPin = "",
                        authenticationState = AuthenticationState.UNLOCKED,
                        authenticationMessage = "Хранилище готово к работе",
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun updateLoginPin(value: String) {
        _uiState.update { it.copy(loginPin = value.onlyDigits(6), errorMessage = null) }
    }

    fun loginWithPin() {
        val pin = _uiState.value.loginPin
        if (accountRepository.verifyPin(pin)) {
            unlockVault("Вход выполнен")
        } else {
            _uiState.update { it.copy(errorMessage = "Неверный PIN-код") }
        }
    }

    fun requestVaultUnlock() {
        _uiState.update {
            when {
                !it.hasAccount -> it.copy(errorMessage = "Сначала зарегистрируйтесь")
                it.biometricAvailability == BiometricAvailability.AVAILABLE -> it.copy(
                    isBiometricPromptRequested = true,
                    authenticationMessage = "Подтвердите вход биометрией"
                )
                else -> it.copy(authenticationMessage = "Биометрия недоступна, используйте PIN-код")
            }
        }
    }

    fun onBiometricAuthenticationSucceeded() {
        if (_uiState.value.hasAccount) {
            unlockVault("Вход по биометрии выполнен")
        }
    }

    fun onBiometricAuthenticationFailed() {
        _uiState.update {
            it.copy(authenticationMessage = "Биометрическая проверка не прошла")
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
            it.copy(
                authenticationState = AuthenticationState.LOCKED,
                loginPin = "",
                visiblePasswordIds = emptySet(),
                exportedBackupText = "",
                autofillProfile = it.autofillProfile.copy(selectedSuggestion = null),
                authenticationMessage = "Хранилище заблокировано"
            )
        }
    }

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(selectedTab = tab, errorMessage = null, backupMessage = null) }
    }

    fun onSearchChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onCategorySelected(category: PasswordCategory?) {
        _uiState.update { it.copy(selectedCategory = category) }
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
                selectedTab = AppTab.PASSWORDS,
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
                )
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
            it.copy(
                autofillProfile = it.autofillProfile.copy(selectedSuggestion = suggestion),
                authenticationMessage = "Автозаполнение подготовлено для ${suggestion.serviceName}"
            )
        }
    }

    fun clearAutofillSelection() {
        _uiState.update {
            it.copy(autofillProfile = it.autofillProfile.copy(selectedSuggestion = null))
        }
    }

    fun exportEncryptedBackup() {
        _uiState.update {
            it.copy(
                exportedBackupText = repository.exportEncryptedBackup(),
                backupMessage = "Зашифрованная резервная копия создана"
            )
        }
    }

    fun updateImportBackupText(value: String) {
        _uiState.update {
            it.copy(importBackupText = value, backupMessage = null)
        }
    }

    fun importEncryptedBackup() {
        _uiState.update {
            if (it.importBackupText.isBlank()) {
                return@update it.copy(backupMessage = "Вставьте текст резервной копии")
            }

            try {
                val updatedPasswords = repository.importEncryptedBackup(it.importBackupText)
                it.copy(
                    passwords = updatedPasswords,
                    visiblePasswordIds = emptySet(),
                    exportedBackupText = "",
                    autofillProfile = it.autofillProfile.copy(selectedSuggestion = null),
                    autofillSuggestions = buildAutofillSuggestions(
                        requestedService = it.autofillProfile.requestedService,
                        passwords = updatedPasswords
                    ),
                    backupMessage = "Резервная копия импортирована"
                )
            } catch (exception: Exception) {
                it.copy(
                    backupMessage = "Не удалось импортировать: ${exception.message ?: "неверный формат"}"
                )
            }
        }
    }

    fun clearExportedBackup() {
        _uiState.update { it.copy(exportedBackupText = "", backupMessage = null) }
    }

    private fun unlockVault(message: String) {
        _uiState.update {
            it.copy(
                authenticationState = AuthenticationState.UNLOCKED,
                isBiometricPromptRequested = false,
                loginPin = "",
                authenticationMessage = message,
                errorMessage = null
            )
        }
    }

    private fun updateForm(reducer: (PasswordFormState) -> PasswordFormState) {
        _uiState.update {
            it.copy(formState = reducer(it.formState), errorMessage = null)
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

    private fun String.onlyDigits(maxLength: Int): String {
        return filter { it.isDigit() }.take(maxLength)
    }

    private companion object {
        const val MIN_PIN_LENGTH = 4
    }
}
