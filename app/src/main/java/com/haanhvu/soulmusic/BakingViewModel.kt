package com.haanhvu.soulmusic

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
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

    val recordingTitleLink = mutableMapOf<String, String>()

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

                response.text?.let { outputContent ->
                    Log.e("BakingViewModel", "Gemini output: " + outputContent)
                    val tags = outputContent.split(",").toTypedArray()
                    for (t in tags) {
                        val recordingsResult = RetrofitClient.api.searchSongsByTag("tag:" + t)
                        Log.e("BakingViewModel", "Recording result: " + recordingsResult)
                        if (recordingsResult.recordings.size == 0) {
                            recordingTitleLink["Some title"] = "Some link"
                            continue
                        }
                        val recordingUrlsResult = RetrofitClient.api.getRecordingUrls(recordingsResult.recordings[0].id)
                        val recordingTitle = recordingsResult.recordings[0].title
                        val recordingLink = if (recordingUrlsResult.relations.size > 0) {
                            recordingUrlsResult.relations[0].url.resource
                        } else {
                            "Not found. Please submit if you find."
                        }
                        Log.e("BakingViewModel", "Recording link: " + recordingLink)
                        recordingTitleLink[recordingTitle] = recordingLink
                    }
                    _uiState.value = UiState.Success(recordingTitleLink)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "" + " at BakingViewModel.")
            }
        }
    }
}