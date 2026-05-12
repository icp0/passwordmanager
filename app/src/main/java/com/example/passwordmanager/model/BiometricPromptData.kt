package com.example.passwordmanager.model

data class BiometricPromptData(
    val title: String = "Разблокировать хранилище",
    val subtitle: String = "Используйте отпечаток пальца или распознавание лица",
    val negativeButtonText: String = "Отмена"
)
