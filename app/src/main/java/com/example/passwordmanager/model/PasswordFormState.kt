package com.example.passwordmanager.model

data class PasswordFormState(
    val serviceName: String = "",
    val username: String = "",
    val password: String = "",
    val category: PasswordCategory = PasswordCategory.PERSONAL,
    val note: String = ""
)
