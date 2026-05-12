package com.example.passwordmanager.model

import java.util.UUID

class PasswordRepository {
    private val cryptoManager = PasswordCryptoManager()
    private val passwords = mutableListOf<EncryptedPasswordEntry>()

    init {
        addEncryptedPassword(
            serviceName = "GitHub",
            username = "student@example.com",
            password = "Hackathon2026!",
            category = PasswordCategory.WORK,
            note = "Demo account"
        )
        addEncryptedPassword(
            serviceName = "Email",
            username = "student@mail.com",
            password = "Compose#12345",
            category = PasswordCategory.PERSONAL
        )
    }

    fun getPasswords(): List<PasswordEntry> {
        return passwords.map { encryptedEntry ->
            PasswordEntry(
                id = encryptedEntry.id,
                serviceName = encryptedEntry.serviceName,
                username = encryptedEntry.username,
                password = cryptoManager.decrypt(encryptedEntry.encryptedPassword),
                category = encryptedEntry.category,
                note = encryptedEntry.note
            )
        }
    }

    fun addPassword(formState: PasswordFormState): List<PasswordEntry> {
        addEncryptedPassword(
            serviceName = formState.serviceName.trim(),
            username = formState.username.trim(),
            password = formState.password,
            category = formState.category,
            note = formState.note.trim()
        )
        return getPasswords()
    }

    fun deletePassword(id: String): List<PasswordEntry> {
        passwords.removeAll { it.id == id }
        return getPasswords()
    }

    private fun addEncryptedPassword(
        serviceName: String,
        username: String,
        password: String,
        category: PasswordCategory,
        note: String = ""
    ) {
        passwords.add(
            EncryptedPasswordEntry(
                id = UUID.randomUUID().toString(),
                serviceName = serviceName,
                username = username,
                encryptedPassword = cryptoManager.encrypt(password),
                category = category,
                note = note
            )
        )
    }
}
