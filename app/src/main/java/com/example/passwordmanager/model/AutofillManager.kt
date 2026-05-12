package com.example.passwordmanager.model

class AutofillManager {
    fun findSuggestions(
        requestedService: String,
        passwords: List<PasswordEntry>
    ): List<AutofillSuggestion> {
        val normalizedRequest = requestedService.trim()

        return passwords
            .filter { entry ->
                normalizedRequest.isBlank() ||
                    entry.serviceName.contains(normalizedRequest, ignoreCase = true)
            }
            .map { entry ->
                AutofillSuggestion(
                    entryId = entry.id,
                    serviceName = entry.serviceName,
                    username = entry.username,
                    password = entry.password,
                    displayLabel = "${entry.serviceName} - ${entry.username}"
                )
            }
    }
}
