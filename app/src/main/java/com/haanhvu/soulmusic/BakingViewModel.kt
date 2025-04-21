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

    private val recordingsResult = arrayOfNulls<MusicBrainzResponse>(5)

    val recordingTitleLink = mutableMapOf<String, String>()

    private var iteration = 0

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
                    for (i in tags.indices) {
                        recordingsResult[i] = RetrofitClient.api.searchSongsByTag("tag:" + tags[i])
                        val recordingsResultItem = recordingsResult[i]
                        recordingsResultItem?.let {
                            Log.e("BakingViewModel", "Recordings result: " + recordingsResultItem)
                            Log.e("BakingViewModel", "Number of recordings: " + recordingsResultItem.recordings.size)
                            var index = 0
                            while (recordingsResultItem.recordings.size > 0 && index < recordingsResultItem.recordings.size) {
                                Log.e("BakingViewModel", "Index: " + index)
                                val recordingUrlsResult = RetrofitClient.api.getRecordingUrls(recordingsResultItem.recordings[index].id)
                                if (recordingUrlsResult.relations.size > 0) {
                                    val recordingTitle = recordingsResultItem.recordings[index].title
                                    val recordingLink = recordingUrlsResult.relations[0].url.resource
                                    recordingTitleLink[recordingTitle] = recordingLink
                                    break
                                } else {
                                    var query = recordingsResultItem.recordings[index].title
                                    var artistName = ""
                                    for (artist in recordingsResultItem.recordings[index].artistCredit) {
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
                                    val recordingTitle = recordingsResultItem.recordings[index].title
                                    recordingTitleLink[recordingTitle + " by" + artistName] = recordingLink
                                    break
                                }
                                index++
                            }
                        }
                    }
                    _uiState.value = UiState.Success(recordingTitleLink)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "" + " at BakingViewModel.")
            }
        }
    }

    fun addMoreResults() {
        iteration++


    }
}