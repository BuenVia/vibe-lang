package com.vibelang.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vibelang.android.ui.VibeLangApp
import com.vibelang.android.ui.theme.VibeLangTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VibeLangTheme {
                VibeLangApp()
            }
        }
    }
}
