package com.example.passwordmanager.model

data class AutofillProfile(
    val requestedService: String = "",
    val selectedSuggestion: AutofillSuggestion? = null,
    val isAutofillEnabled: Boolean = true
)
