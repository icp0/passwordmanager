package com.example.passwordmanager.model

data class EncryptedData(
    val cipherText: String,
    val initializationVector: String
)
