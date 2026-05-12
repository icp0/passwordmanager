package com.example.passwordmanager.model

data class PasswordEntry(
    val id: String,
    val serviceName: String,
    val username: String,
    val password: String,
    val category: PasswordCategory,
    val note: String = ""
)
