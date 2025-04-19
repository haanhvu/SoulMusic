package com.haanhvu.soulmusic

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

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

        recordingTitleLink.clear()

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
                        Log.e("BakingViewModel", "Recordings result: " + recordingsResult)
                        Log.e("BakingViewModel", "Number of recordings: " + recordingsResult.recordings.size)
                        if (recordingsResult.recordings.size == 0) {
                            recordingTitleLink["Some title"] = "Some link"
                            continue
                        }

                        /*val recordingTitle = recordingsResult.recordings[0].title
                        var recordingLink: String
                        if (recordingsResult.recordings[0].artistCredit != null) {
                            recordingLink = "Artist exists"
                        } else {
                            recordingLink = "No artist"
                        }
                        recordingTitleLink[recordingTitle] = recordingLink*/
                        var index = 0
                        while (index < recordingsResult.recordings.size) {
                            Log.e("BakingViewModel", "Index: " + index)
                            val recordingUrlsResult = RetrofitClient.api.getRecordingUrls(recordingsResult.recordings[index].id)
                            if (recordingUrlsResult.relations.size > 0) {
                                val recordingTitle = recordingsResult.recordings[index].title
                                val recordingLink = recordingUrlsResult.relations[0].url.resource
                                recordingTitleLink[recordingTitle] = recordingLink
                                break
                            } else {
                                var query = recordingsResult.recordings[index].title
                                var artistName = ""
                                for (artist in recordingsResult.recordings[index].artistCredit) {
                                    artistName = artistName + " " + artist.name
                                    query = query + " " + artist.name
                                }
                                val retrofit = Retrofit.Builder()
                                    .baseUrl("https://www.googleapis.com/youtube/v3/")
                                    .addConverterFactory(MoshiConverterFactory.create(RetrofitClient.moshi))
                                    .build()
                                val youTubeApiService = retrofit.create(YouTubeApiService::class.java)
                                val response = youTubeApiService.searchVideos(
                                    query = query
                                )
                                val video = response.items.firstOrNull()
                                var recordingLink = "Not found on Youtube"
                                video?.let {
                                    recordingLink = "https://www.youtube.com/watch?v=${it.id.videoId}"
                                }
                                val recordingTitle = recordingsResult.recordings[index].title
                                recordingTitleLink[recordingTitle + " by" + artistName] = recordingLink
                                break
                            }
                            index++
                        }
                    }
                    _uiState.value = UiState.Success(recordingTitleLink)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "" + " at BakingViewModel.")
            }
        }
    }
}