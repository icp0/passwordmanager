package com.example.passwordmanager.model

import java.util.UUID

class PasswordRepository {
    private val passwords = mutableListOf(
        PasswordEntry(
            id = UUID.randomUUID().toString(),
            serviceName = "GitHub",
            username = "student@example.com",
            password = "Hackathon2026!",
            category = PasswordCategory.WORK,
            note = "Demo account"
        ),
        PasswordEntry(
            id = UUID.randomUUID().toString(),
            serviceName = "Email",
            username = "student@mail.com",
            password = "Compose#12345",
            category = PasswordCategory.PERSONAL
        )
    )

    fun getPasswords(): List<PasswordEntry> = passwords.toList()

    fun addPassword(formState: PasswordFormState): List<PasswordEntry> {
        passwords.add(
            PasswordEntry(
                id = UUID.randomUUID().toString(),
                serviceName = formState.serviceName.trim(),
                username = formState.username.trim(),
                password = formState.password,
                category = formState.category,
                note = formState.note.trim()
            )
        )
        return getPasswords()
    }

    fun deletePassword(id: String): List<PasswordEntry> {
        passwords.removeAll { it.id == id }
        return getPasswords()
    }
}
