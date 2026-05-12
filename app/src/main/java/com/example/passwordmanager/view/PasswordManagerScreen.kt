package com.example.passwordmanager.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.passwordmanager.model.AuthenticationState
import com.example.passwordmanager.model.AutofillSuggestion
import com.example.passwordmanager.model.BiometricAccessManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.passwordmanager.model.PasswordCategory
import com.example.passwordmanager.model.PasswordEntry
import com.example.passwordmanager.model.PasswordFormState
import com.example.passwordmanager.view.ui.theme.PasswordmanagerTheme
import com.example.passwordmanager.viewmodel.PasswordManagerUiState
import com.example.passwordmanager.viewmodel.PasswordManagerViewModel

@Composable
fun PasswordManagerScreen(
    viewModel: PasswordManagerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.onBiometricAvailabilityChecked(
            BiometricAccessManager().getAvailability(context)
        )
    }

    if (uiState.isBiometricPromptRequested) {
        LaunchedEffect(uiState.isBiometricPromptRequested) {
            showBiometricPrompt(
                activity = context as? FragmentActivity,
                title = uiState.biometricPromptData.title,
                subtitle = uiState.biometricPromptData.subtitle,
                negativeButtonText = uiState.biometricPromptData.negativeButtonText,
                onSuccess = viewModel::onBiometricAuthenticationSucceeded,
                onFailed = viewModel::onBiometricAuthenticationFailed,
                onError = viewModel::onBiometricAuthenticationError
            )
        }
    }

    PasswordManagerContent(
        uiState = uiState,
        onSearchChanged = viewModel::onSearchChanged,
        onCategorySelected = viewModel::onCategorySelected,
        onRequestUnlock = viewModel::requestVaultUnlock,
        onLockVault = viewModel::lockVault,
        onAutofillServiceChanged = viewModel::updateAutofillService,
        onAutofillSuggestionSelected = viewModel::selectAutofillSuggestion,
        onClearAutofillSelection = viewModel::clearAutofillSelection,
        onToggleForm = viewModel::toggleFormVisibility,
        onServiceNameChanged = viewModel::updateServiceName,
        onUsernameChanged = viewModel::updateUsername,
        onPasswordChanged = viewModel::updatePassword,
        onEntryCategoryChanged = viewModel::updateCategory,
        onNoteChanged = viewModel::updateNote,
        onGeneratePassword = viewModel::generatePassword,
        onAddPassword = viewModel::addPassword,
        onDeletePassword = viewModel::deletePassword,
        onTogglePasswordVisibility = viewModel::togglePasswordVisibility
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasswordManagerContent(
    uiState: PasswordManagerUiState,
    onSearchChanged: (String) -> Unit,
    onCategorySelected: (PasswordCategory?) -> Unit,
    onRequestUnlock: () -> Unit,
    onLockVault: () -> Unit,
    onAutofillServiceChanged: (String) -> Unit,
    onAutofillSuggestionSelected: (AutofillSuggestion) -> Unit,
    onClearAutofillSelection: () -> Unit,
    onToggleForm: () -> Unit,
    onServiceNameChanged: (String) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onEntryCategoryChanged: (PasswordCategory) -> Unit,
    onNoteChanged: (String) -> Unit,
    onGeneratePassword: () -> Unit,
    onAddPassword: () -> Unit,
    onDeletePassword: (String) -> Unit,
    onTogglePasswordVisibility: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Менеджер паролей",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    TextButton(onClick = onToggleForm) {
                        Text(if (uiState.isFormVisible) "Закрыть" else "Добавить")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SecurityAndAutofillPanel(
                    uiState = uiState,
                    onRequestUnlock = onRequestUnlock,
                    onLockVault = onLockVault,
                    onAutofillServiceChanged = onAutofillServiceChanged,
                    onAutofillSuggestionSelected = onAutofillSuggestionSelected,
                    onClearAutofillSelection = onClearAutofillSelection
                )
            }

            item {
                SearchAndFilters(
                    searchQuery = uiState.searchQuery,
                    selectedCategory = uiState.selectedCategory,
                    onSearchChanged = onSearchChanged,
                    onCategorySelected = onCategorySelected
                )
            }

            if (uiState.isFormVisible) {
                item {
                    PasswordForm(
                        formState = uiState.formState,
                        errorMessage = uiState.errorMessage,
                        onServiceNameChanged = onServiceNameChanged,
                        onUsernameChanged = onUsernameChanged,
                        onPasswordChanged = onPasswordChanged,
                        onCategoryChanged = onEntryCategoryChanged,
                        onNoteChanged = onNoteChanged,
                        onGeneratePassword = onGeneratePassword,
                        onAddPassword = onAddPassword
                    )
                }
            }

            if (uiState.filteredPasswords.isEmpty()) {
                item {
                    EmptyState()
                }
            } else {
                items(
                    items = uiState.filteredPasswords,
                    key = { it.id }
                ) { entry ->
                    PasswordCard(
                        entry = entry,
                        isPasswordVisible = entry.id in uiState.visiblePasswordIds,
                        onTogglePasswordVisibility = { onTogglePasswordVisibility(entry.id) },
                        onDelete = { onDeletePassword(entry.id) }
                    )
                }
            }
        }
    }
}

private fun showBiometricPrompt(
    activity: FragmentActivity?,
    title: String,
    subtitle: String,
    negativeButtonText: String,
    onSuccess: () -> Unit,
    onFailed: () -> Unit,
    onError: (String) -> Unit
) {
    if (activity == null) {
        onError("Для биометрии требуется FragmentActivity")
        return
    }

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setNegativeButtonText(negativeButtonText)
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        .build()

    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationFailed() {
                onFailed()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError(errString.toString())
            }
        }
    )

    prompt.authenticate(promptInfo)
}

@Composable
private fun SecurityAndAutofillPanel(
    uiState: PasswordManagerUiState,
    onRequestUnlock: () -> Unit,
    onLockVault: () -> Unit,
    onAutofillServiceChanged: (String) -> Unit,
    onAutofillSuggestionSelected: (AutofillSuggestion) -> Unit,
    onClearAutofillSelection: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Безопасность",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${uiState.authenticationState.toRussianText()} - ${uiState.authenticationMessage}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (uiState.authenticationState == AuthenticationState.UNLOCKED) {
                    OutlinedButton(onClick = onLockVault) {
                        Text("Заблокировать")
                    }
                } else {
                    Button(
                        onClick = onRequestUnlock,
                        enabled = uiState.authenticationState != AuthenticationState.UNAVAILABLE
                    ) {
                        Text("Разблокировать")
                    }
                }
            }

            OutlinedTextField(
                value = uiState.autofillProfile.requestedService,
                onValueChange = onAutofillServiceChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Сервис для автозаполнения") },
                singleLine = true
            )

            if (uiState.autofillSuggestions.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.autofillSuggestions.forEach { suggestion ->
                        FilterChip(
                            selected = uiState.autofillProfile.selectedSuggestion?.entryId == suggestion.entryId,
                            onClick = { onAutofillSuggestionSelected(suggestion) },
                            label = { Text(suggestion.displayLabel) }
                        )
                    }
                }
            }

            uiState.autofillProfile.selectedSuggestion?.let { suggestion ->
                Text(
                    text = "Автозаполнение готово: ${suggestion.username} / ${suggestion.password}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                TextButton(onClick = onClearAutofillSelection) {
                    Text("Очистить автозаполнение")
                }
            }
        }
    }
}

private fun AuthenticationState.toRussianText(): String {
    return when (this) {
        AuthenticationState.LOCKED -> "Заблокировано"
        AuthenticationState.UNLOCKED -> "Разблокировано"
        AuthenticationState.UNAVAILABLE -> "Недоступно"
    }
}

@Composable
private fun SearchAndFilters(
    searchQuery: String,
    selectedCategory: PasswordCategory?,
    onSearchChanged: (String) -> Unit,
    onCategorySelected: (PasswordCategory?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Поиск") },
            singleLine = true
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("Все") }
            )
            PasswordCategory.entries.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                    label = { Text(category.title) }
                )
            }
        }
    }
}

@Composable
private fun PasswordForm(
    formState: PasswordFormState,
    errorMessage: String?,
    onServiceNameChanged: (String) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onCategoryChanged: (PasswordCategory) -> Unit,
    onNoteChanged: (String) -> Unit,
    onGeneratePassword: () -> Unit,
    onAddPassword: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Новый пароль",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = formState.serviceName,
                onValueChange = onServiceNameChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Сервис") },
                singleLine = true
            )
            OutlinedTextField(
                value = formState.username,
                onValueChange = onUsernameChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Логин или email") },
                singleLine = true
            )
            OutlinedTextField(
                value = formState.password,
                onValueChange = onPasswordChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Пароль") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PasswordCategory.entries.forEach { category ->
                    FilterChip(
                        selected = formState.category == category,
                        onClick = { onCategoryChanged(category) },
                        label = { Text(category.title) }
                    )
                }
            }
            OutlinedTextField(
                value = formState.note,
                onValueChange = onNoteChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Заметка") },
                minLines = 2
            )
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onGeneratePassword) {
                    Text("Сгенерировать")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onAddPassword) {
                    Text("Сохранить")
                }
            }
        }
    }
}

@Composable
private fun PasswordCard(
    entry: PasswordEntry,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.serviceName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = entry.category.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                TextButton(onClick = onDelete) {
                    Text("Удалить")
                }
            }
            Text(
                text = entry.username,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isPasswordVisible) entry.password else "********",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                TextButton(onClick = onTogglePasswordVisibility) {
                    Text(if (isPasswordVisible) "Скрыть" else "Показать")
                }
            }
            if (entry.note.isNotBlank()) {
                Text(
                    text = entry.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Пароли не найдены",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Добавьте запись или измените фильтры поиска",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PasswordManagerPreview() {
    PasswordmanagerTheme {
        PasswordManagerContent(
            uiState = PasswordManagerUiState(
                passwords = listOf(
                    PasswordEntry(
                        id = "1",
                        serviceName = "GitHub",
                        username = "student@example.com",
                        password = "Hackathon2026!",
                        category = PasswordCategory.WORK,
                        note = "Демо-запись"
                    )
                )
            ),
            onSearchChanged = {},
            onCategorySelected = {},
            onRequestUnlock = {},
            onLockVault = {},
            onAutofillServiceChanged = {},
            onAutofillSuggestionSelected = {},
            onClearAutofillSelection = {},
            onToggleForm = {},
            onServiceNameChanged = {},
            onUsernameChanged = {},
            onPasswordChanged = {},
            onEntryCategoryChanged = {},
            onNoteChanged = {},
            onGeneratePassword = {},
            onAddPassword = {},
            onDeletePassword = {},
            onTogglePasswordVisibility = {}
        )
    }
}
