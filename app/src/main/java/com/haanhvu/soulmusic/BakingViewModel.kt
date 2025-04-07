package com.haanhvu.soulmusic

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BakingViewModel : ViewModel() {
    private val _uiState: MutableStateFlow<UiState> =
        MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> =
        _uiState.asStateFlow()

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.apiKey
    )

    fun sendPrompt(
        bitmap: Bitmap,
        prompt: String
    ) {
        _uiState.value = UiState.Loading

        val newPrompt = prompt + ". What music moods would help me in this case? Only answer the music moods that would help me in this case separated by comma, nothing else.";

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Problem lies here
                val musicBrainzResponse = RetrofitClient.api.searchSongsByTag("rock")

                val response = generativeModel.generateContent(
                    content {
                        image(bitmap)
                        //text(newPrompt)
                        text("Is this a title of a song? " + musicBrainzResponse.recordings[0].title)
                    }
                )

                response.text?.let { outputContent ->
                    _uiState.value = UiState.Success(musicBrainzResponse.recordings[0].title)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "")
            }
        }
    }
}