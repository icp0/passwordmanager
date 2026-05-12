package com.example.passwordmanager

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.example.passwordmanager.view.PasswordManagerScreen
import com.example.passwordmanager.view.ui.theme.PasswordmanagerTheme

class MainActivity : FragmentActivity() {
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
