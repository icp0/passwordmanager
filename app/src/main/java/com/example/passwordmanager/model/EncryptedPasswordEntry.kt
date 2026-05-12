package com.example.passwordmanager.model

data class EncryptedPasswordEntry(
    val id: String,
    val serviceName: String,
    val username: String,
    val encryptedPassword: EncryptedData,
    val category: PasswordCategory,
    val note: String = ""
)
