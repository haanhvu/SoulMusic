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

    val recordings = mutableMapOf<String, String>()

    fun sendPrompt(
        prompt: String
    ) {
        _uiState.value = UiState.Loading

        val newPrompt = prompt + ". Give me five keywords to search for music that would make me better in this case. Only answer those keywords, separated by commas.";

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = generativeModel.generateContent(
                    content {
                        text(newPrompt)
                    }
                )

                /*response.text?.let { outputContent ->
                    _uiState.value = UiState.Success(outputContent)
                }*/
                response.text?.let { outputContent ->
                    val tags = outputContent.split(",").toTypedArray()
                    for (t in tags) {
                        val musicBrainzResult = RetrofitClient.api.searchSongsByTag("tag:" + t)
                        
                        _uiState.value = UiState.Success(musicBrainzResult.recordings[0].title)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "")
            }
        }
    }
}