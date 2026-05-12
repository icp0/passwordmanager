package com.example.passwordmanager.model

data class AutofillSuggestion(
    val entryId: String,
    val serviceName: String,
    val username: String,
    val password: String,
    val displayLabel: String
)
