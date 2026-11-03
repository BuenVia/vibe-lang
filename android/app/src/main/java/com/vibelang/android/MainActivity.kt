package com.vibelang.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.vibelang.android.ui.VibeLangApp
import com.vibelang.android.ui.theme.VibeLangTheme

class MainActivity : ComponentActivity() {
    /** Text shared from another app (e.g. a chat message), shown in Add mistake. */
    private val sharedText = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) sharedText.value = intent.sharedText()
        setContent {
            VibeLangTheme {
                VibeLangApp(sharedText = sharedText.value, onSharedTextHandled = { sharedText.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.sharedText()?.let { sharedText.value = it }
    }

    private fun Intent.sharedText(): String? =
        if (action == Intent.ACTION_SEND && type?.startsWith("text/") == true) getStringExtra(Intent.EXTRA_TEXT) else null
}
