package com.example.passwordmanager.view

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.passwordmanager.model.AuthenticationState
import com.example.passwordmanager.model.AutofillSuggestion
import com.example.passwordmanager.model.BiometricAccessManager
import com.example.passwordmanager.model.BiometricAvailability
import com.example.passwordmanager.model.PasswordCategory
import com.example.passwordmanager.model.PasswordEntry
import com.example.passwordmanager.model.PasswordFormState
import com.example.passwordmanager.view.ui.theme.PasswordmanagerTheme
import com.example.passwordmanager.viewmodel.AppTab
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

    when {
        !uiState.hasAccount -> RegistrationScreen(
            uiState = uiState,
            onPinChanged = viewModel::updateRegistrationPin,
            onPinRepeatChanged = viewModel::updateRegistrationPinRepeat,
            onRegister = viewModel::registerAccount
        )

        !uiState.isUnlocked -> LoginScreen(
            uiState = uiState,
            onPinChanged = viewModel::updateLoginPin,
            onLogin = viewModel::loginWithPin,
            onBiometricLogin = viewModel::requestVaultUnlock
        )

        else -> VaultScreen(
            uiState = uiState,
            onTabSelected = viewModel::selectTab,
            onLockVault = viewModel::lockVault,
            onSearchChanged = viewModel::onSearchChanged,
            onCategorySelected = viewModel::onCategorySelected,
            onServiceNameChanged = viewModel::updateServiceName,
            onUsernameChanged = viewModel::updateUsername,
            onPasswordChanged = viewModel::updatePassword,
            onEntryCategoryChanged = viewModel::updateCategory,
            onNoteChanged = viewModel::updateNote,
            onGeneratePassword = viewModel::generatePassword,
            onAddPassword = viewModel::addPassword,
            onDeletePassword = viewModel::deletePassword,
            onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
            onAutofillServiceChanged = viewModel::updateAutofillService,
            onAutofillSuggestionSelected = viewModel::selectAutofillSuggestion,
            onClearAutofillSelection = viewModel::clearAutofillSelection,
            onExportBackup = viewModel::exportEncryptedBackup,
            onImportBackupTextChanged = viewModel::updateImportBackupText,
            onImportBackup = viewModel::importEncryptedBackup,
            onClearExportedBackup = viewModel::clearExportedBackup
        )
    }
}

@Composable
private fun RegistrationScreen(
    uiState: PasswordManagerUiState,
    onPinChanged: (String) -> Unit,
    onPinRepeatChanged: (String) -> Unit,
    onRegister: () -> Unit
) {
    AuthSurface {
        Text(
            text = "Создайте хранилище",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "PIN-код будет запасным способом входа, если биометрия недоступна.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        PinField(
            value = uiState.registrationPin,
            label = "PIN-код",
            onValueChange = onPinChanged
        )
        PinField(
            value = uiState.registrationPinRepeat,
            label = "Повторите PIN-код",
            onValueChange = onPinRepeatChanged
        )
        StatusText(uiState.errorMessage ?: uiState.authenticationMessage)
        Button(
            onClick = onRegister,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Зарегистрироваться")
        }
    }
}

@Composable
private fun LoginScreen(
    uiState: PasswordManagerUiState,
    onPinChanged: (String) -> Unit,
    onLogin: () -> Unit,
    onBiometricLogin: () -> Unit
) {
    AuthSurface {
        Text(
            text = "Вход",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Войдите по PIN-коду или используйте отпечаток пальца / распознавание лица.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        PinField(
            value = uiState.loginPin,
            label = "PIN-код",
            onValueChange = onPinChanged
        )
        StatusText(uiState.errorMessage ?: uiState.authenticationMessage)
        Button(
            onClick = onLogin,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Войти по PIN")
        }
        OutlinedButton(
            onClick = onBiometricLogin,
            enabled = uiState.biometricAvailability == BiometricAvailability.AVAILABLE,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Войти по биометрии")
        }
    }
}

@Composable
private fun AuthSurface(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultScreen(
    uiState: PasswordManagerUiState,
    onTabSelected: (AppTab) -> Unit,
    onLockVault: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onCategorySelected: (PasswordCategory?) -> Unit,
    onServiceNameChanged: (String) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onEntryCategoryChanged: (PasswordCategory) -> Unit,
    onNoteChanged: (String) -> Unit,
    onGeneratePassword: () -> Unit,
    onAddPassword: () -> Unit,
    onDeletePassword: (String) -> Unit,
    onTogglePasswordVisibility: (String) -> Unit,
    onAutofillServiceChanged: (String) -> Unit,
    onAutofillSuggestionSelected: (AutofillSuggestion) -> Unit,
    onClearAutofillSelection: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackupTextChanged: (String) -> Unit,
    onImportBackup: () -> Unit,
    onClearExportedBackup: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Менеджер паролей",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${uiState.passwords.size} записей",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onLockVault) {
                        Text("Выйти")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            TabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
                AppTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        text = { Text(tab.title) }
                    )
                }
            }

            when (uiState.selectedTab) {
                AppTab.PASSWORDS -> PasswordsTab(
                    uiState = uiState,
                    onSearchChanged = onSearchChanged,
                    onCategorySelected = onCategorySelected,
                    onDeletePassword = onDeletePassword,
                    onTogglePasswordVisibility = onTogglePasswordVisibility,
                    onAutofillServiceChanged = onAutofillServiceChanged,
                    onAutofillSuggestionSelected = onAutofillSuggestionSelected,
                    onClearAutofillSelection = onClearAutofillSelection
                )

                AppTab.ADD_PASSWORD -> AddPasswordTab(
                    uiState = uiState,
                    onServiceNameChanged = onServiceNameChanged,
                    onUsernameChanged = onUsernameChanged,
                    onPasswordChanged = onPasswordChanged,
                    onCategoryChanged = onEntryCategoryChanged,
                    onNoteChanged = onNoteChanged,
                    onGeneratePassword = onGeneratePassword,
                    onAddPassword = onAddPassword
                )

                AppTab.BACKUP -> BackupTab(
                    uiState = uiState,
                    onExportBackup = onExportBackup,
                    onImportBackupTextChanged = onImportBackupTextChanged,
                    onImportBackup = onImportBackup,
                    onClearExportedBackup = onClearExportedBackup
                )
            }
        }
    }
}

@Composable
private fun PasswordsTab(
    uiState: PasswordManagerUiState,
    onSearchChanged: (String) -> Unit,
    onCategorySelected: (PasswordCategory?) -> Unit,
    onDeletePassword: (String) -> Unit,
    onTogglePasswordVisibility: (String) -> Unit,
    onAutofillServiceChanged: (String) -> Unit,
    onAutofillSuggestionSelected: (AutofillSuggestion) -> Unit,
    onClearAutofillSelection: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            MinimalPanel {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Поиск") },
                    singleLine = true
                )
                CategoryFilters(
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = onCategorySelected
                )
            }
        }
        item {
            AutofillPanel(
                uiState = uiState,
                onAutofillServiceChanged = onAutofillServiceChanged,
                onAutofillSuggestionSelected = onAutofillSuggestionSelected,
                onClearAutofillSelection = onClearAutofillSelection
            )
        }
        if (uiState.filteredPasswords.isEmpty()) {
            item { EmptyState() }
        } else {
            items(uiState.filteredPasswords, key = { it.id }) { entry ->
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

@Composable
private fun AddPasswordTab(
    uiState: PasswordManagerUiState,
    onServiceNameChanged: (String) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onCategoryChanged: (PasswordCategory) -> Unit,
    onNoteChanged: (String) -> Unit,
    onGeneratePassword: () -> Unit,
    onAddPassword: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            MinimalPanel {
                Text(
                    text = "Новая запись",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = uiState.formState.serviceName,
                    onValueChange = onServiceNameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Сервис") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.formState.username,
                    onValueChange = onUsernameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Логин или email") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.formState.password,
                    onValueChange = onPasswordChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Пароль") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                CategoryFilters(
                    selectedCategory = uiState.formState.category,
                    onCategorySelected = { category ->
                        if (category != null) onCategoryChanged(category)
                    }
                )
                OutlinedTextField(
                    value = uiState.formState.note,
                    onValueChange = onNoteChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Заметка") },
                    minLines = 2
                )
                uiState.errorMessage?.let { StatusText(it, isError = true) }
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
}

@Composable
private fun BackupTab(
    uiState: PasswordManagerUiState,
    onExportBackup: () -> Unit,
    onImportBackupTextChanged: (String) -> Unit,
    onImportBackup: () -> Unit,
    onClearExportedBackup: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            MinimalPanel {
                Text(
                    text = "Резервная копия",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onExportBackup) {
                        Text("Экспорт")
                    }
                    OutlinedButton(
                        onClick = onClearExportedBackup,
                        enabled = uiState.exportedBackupText.isNotBlank()
                    ) {
                        Text("Очистить")
                    }
                }
                if (uiState.exportedBackupText.isNotBlank()) {
                    OutlinedTextField(
                        value = uiState.exportedBackupText,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Зашифрованный JSON") },
                        minLines = 4,
                        maxLines = 6,
                        readOnly = true
                    )
                }
                OutlinedTextField(
                    value = uiState.importBackupText,
                    onValueChange = onImportBackupTextChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Вставьте резервную копию") },
                    minLines = 4,
                    maxLines = 6
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onImportBackup) {
                        Text("Импорт")
                    }
                }
                uiState.backupMessage?.let { StatusText(it) }
            }
        }
    }
}

@Composable
private fun AutofillPanel(
    uiState: PasswordManagerUiState,
    onAutofillServiceChanged: (String) -> Unit,
    onAutofillSuggestionSelected: (AutofillSuggestion) -> Unit,
    onClearAutofillSelection: () -> Unit
) {
    MinimalPanel {
        Text(
            text = "Автозаполнение",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        OutlinedTextField(
            value = uiState.autofillProfile.requestedService,
            onValueChange = onAutofillServiceChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Сервис или сайт") },
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
            StatusText("Готово: ${suggestion.username} / ${suggestion.password}")
            TextButton(onClick = onClearAutofillSelection) {
                Text("Сбросить")
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
            Text(entry.username, style = MaterialTheme.typography.bodyMedium)
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
private fun CategoryFilters(
    selectedCategory: PasswordCategory?,
    onCategorySelected: (PasswordCategory?) -> Unit
) {
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

@Composable
private fun PinField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
    )
}

@Composable
private fun MinimalPanel(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun StatusText(
    text: String,
    isError: Boolean = false
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (isError) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    )
}

@Composable
private fun EmptyState() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium
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
                text = "Добавьте запись или измените фильтры",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

@Preview(showBackground = true)
@Composable
private fun PasswordManagerPreview() {
    PasswordmanagerTheme {
        VaultScreen(
            uiState = PasswordManagerUiState(
                hasAccount = true,
                authenticationState = AuthenticationState.UNLOCKED,
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
            onTabSelected = {},
            onLockVault = {},
            onSearchChanged = {},
            onCategorySelected = {},
            onServiceNameChanged = {},
            onUsernameChanged = {},
            onPasswordChanged = {},
            onEntryCategoryChanged = {},
            onNoteChanged = {},
            onGeneratePassword = {},
            onAddPassword = {},
            onDeletePassword = {},
            onTogglePasswordVisibility = {},
            onAutofillServiceChanged = {},
            onAutofillSuggestionSelected = {},
            onClearAutofillSelection = {},
            onExportBackup = {},
            onImportBackupTextChanged = {},
            onImportBackup = {},
            onClearExportedBackup = {}
        )
    }
}
