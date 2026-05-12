package com.example.passwordmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.passwordmanager.view.ui.theme.PasswordmanagerTheme
import com.example.passwordmanager.view.PasswordManagerScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PasswordmanagerTheme {
                PasswordManagerScreen()
            }
        }
    }
}
