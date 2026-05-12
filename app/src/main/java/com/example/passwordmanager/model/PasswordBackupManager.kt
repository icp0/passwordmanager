package com.example.passwordmanager.model

import org.json.JSONArray
import org.json.JSONObject

class PasswordBackupManager {
    fun exportBackup(entries: List<EncryptedPasswordEntry>): String {
        val root = JSONObject()
        val items = JSONArray()

        entries.forEach { entry ->
            val item = JSONObject()
                .put(KEY_ID, entry.id)
                .put(KEY_SERVICE_NAME, entry.serviceName)
                .put(KEY_USERNAME, entry.username)
                .put(KEY_PASSWORD_CIPHER_TEXT, entry.encryptedPassword.cipherText)
                .put(KEY_PASSWORD_IV, entry.encryptedPassword.initializationVector)
                .put(KEY_CATEGORY, entry.category.name)
                .put(KEY_NOTE, entry.note)

            items.put(item)
        }

        return root
            .put(KEY_VERSION, BACKUP_VERSION)
            .put(KEY_ITEMS, items)
            .toString()
    }

    fun importBackup(backupText: String): List<EncryptedPasswordEntry> {
        val root = JSONObject(backupText.trim())
        val version = root.optInt(KEY_VERSION, UNKNOWN_VERSION)

        require(version == BACKUP_VERSION) {
            "Неподдерживаемая версия резервной копии"
        }

        val items = root.getJSONArray(KEY_ITEMS)
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)

                add(
                    EncryptedPasswordEntry(
                        id = item.getString(KEY_ID),
                        serviceName = item.getString(KEY_SERVICE_NAME),
                        username = item.getString(KEY_USERNAME),
                        encryptedPassword = EncryptedData(
                            cipherText = item.getString(KEY_PASSWORD_CIPHER_TEXT),
                            initializationVector = item.getString(KEY_PASSWORD_IV)
                        ),
                        category = PasswordCategory.valueOf(item.getString(KEY_CATEGORY)),
                        note = item.optString(KEY_NOTE)
                    )
                )
            }
        }
    }

    private companion object {
        const val BACKUP_VERSION = 1
        const val UNKNOWN_VERSION = -1
        const val KEY_VERSION = "version"
        const val KEY_ITEMS = "items"
        const val KEY_ID = "id"
        const val KEY_SERVICE_NAME = "serviceName"
        const val KEY_USERNAME = "username"
        const val KEY_PASSWORD_CIPHER_TEXT = "passwordCipherText"
        const val KEY_PASSWORD_IV = "passwordIv"
        const val KEY_CATEGORY = "category"
        const val KEY_NOTE = "note"
    }
}
