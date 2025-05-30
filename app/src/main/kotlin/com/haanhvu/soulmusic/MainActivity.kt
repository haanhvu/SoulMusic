package com.haanhvu.soulmusic

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.haanhvu.soulmusic.ui.theme.SoulMusicTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var apiKeyManager: ApiKeyManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        apiKeyManager = ApiKeyManager(this)

        var apiKey = ""
        CoroutineScope(Dispatchers.Main).launch {
            apiKey = apiKeyManager.fetchApiKey()

            setContent {
                SoulMusicTheme {
                    // A surface container using the 'background' color from the theme
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        SoulMusicView(apiKey)
                    }
                }
            }
        }
    }
}