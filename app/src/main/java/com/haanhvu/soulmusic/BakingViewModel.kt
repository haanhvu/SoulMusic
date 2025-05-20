package com.haanhvu.soulmusic

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import androidx.compose.runtime.snapshots.SnapshotStateList
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

    var recordingTitleLink = mutableMapOf<String, String>()
    private val fullRecordingTitleLink = mutableMapOf<String, String>()

    private val indexes = arrayOfNulls<Int>(5)

    fun sendPromptToAI(
        prompt: String
    ) {
        _uiState.value = UiState.Loading

        recordingTitleLink.clear()

        val newPrompt = prompt + ". Give me twenty results of music that can help me in this case. Only answer title - artist separated by commas, nothing else."

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = generativeModel.generateContent(
                    content {
                        text(newPrompt)
                    }
                )

                response.text?.let { outputContent ->
                    val cleanOutput = outputContent.replace("\"", "")
                    val titleArtist = cleanOutput.split(",")
                    for (item in titleArtist) {
                        val retrofit = Retrofit.Builder()
                            .baseUrl("https://www.googleapis.com/youtube/v3/")
                            .addConverterFactory(MoshiConverterFactory.create(RetrofitClient.moshi))
                            .build()
                        val youTubeApiService = retrofit.create(YouTubeApiService::class.java)
                        val response = youTubeApiService.searchVideos(
                            query = item
                        )
                        val video = response.items.firstOrNull()
                        var recordingLink = "Not found on Youtube"
                        video?.let {
                            recordingLink = "https://www.youtube.com/watch?v=${it.id.videoId}"
                        }
                        fullRecordingTitleLink[item] = recordingLink
                    }
                    recordingTitleLink = fullRecordingTitleLink.entries.take(5).associateTo(mutableMapOf()) { it.toPair() }
                    _uiState.value = UiState.Success(recordingTitleLink)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "" + " at BakingViewModel.")
            }
        }
    }

    fun sendPromptToMusicBrainz(
        prompt: String
    ) {
        _uiState.value = UiState.Loading

        recordingTitleLink.clear()

        val newPrompt = prompt + ". Give me five most accurate keywords to search for the music that can help me in this case. Only answer those keywords, separated by commas.";

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
                                    indexes[i] = index
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
                                    var youtubeVideoTitle = ""
                                    video?.let {
                                        recordingLink = "https://www.youtube.com/watch?v=${it.id.videoId}"
                                        youtubeVideoTitle = it.snippet.title
                                    }
                                    val recordingTitle = recordingsResultItem.recordings[index].title
                                    if (video == null || !youtubeVideoTitle.lowercase().contains(recordingTitle.lowercase())) {
                                        index++
                                        continue
                                    }
                                    recordingTitleLink[recordingTitle + " by" + artistName] = recordingLink
                                    indexes[i] = index
                                    break
                                }
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

    fun addMoreResults(
        shot: Int,
        stateListRecordingTitleLink: SnapshotStateList<Pair<String, String>>
    ) {
        if (!fullRecordingTitleLink.isEmpty()) {
            addMorePopularResults(shot, stateListRecordingTitleLink)
        } else {
            addMoreLesserKnownResults(stateListRecordingTitleLink)
        }
    }

    fun addMorePopularResults(
        shot: Int,
        stateListRecordingTitleLink: SnapshotStateList<Pair<String, String>>
    ) {
        stateListRecordingTitleLink.addAll(fullRecordingTitleLink.entries.toList().subList(shot*5, shot*10).map { it.key to it.value })
    }

    fun addMoreLesserKnownResults(
        stateListRecordingTitleLink: SnapshotStateList<Pair<String, String>>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                for (i in recordingsResult.indices){
                    indexes[i] = indexes[i]!! + 1
                    var r = recordingsResult[i]
                    r?.let {
                        while (r.recordings.size > 0 && indexes[i]!! < r.recordings.size) {
                            val recordingUrlsResult = RetrofitClient.api.getRecordingUrls(r.recordings[indexes[i]!!].id)
                            if (recordingUrlsResult.relations.size > 0) {
                                val recordingTitle = r.recordings[indexes[i]!!].title
                                val recordingLink = recordingUrlsResult.relations[0].url.resource
                                stateListRecordingTitleLink.add(Pair(recordingTitle, recordingLink))
                                break
                            } else {
                                var query = r.recordings[indexes[i]!!].title
                                var artistName = ""
                                for (artist in r.recordings[indexes[i]!!].artistCredit) {
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
                                var youtubeVideoTitle = ""
                                video?.let {
                                    recordingLink = "https://www.youtube.com/watch?v=${it.id.videoId}"
                                    youtubeVideoTitle = it.snippet.title
                                }
                                val recordingTitle = r.recordings[indexes[i]!!].title
                                if (video == null || !youtubeVideoTitle.lowercase().contains(recordingTitle.lowercase())) {
                                    indexes[i] = indexes[i]!! + 1
                                    continue
                                }
                                stateListRecordingTitleLink.add(Pair(recordingTitle + " by" + artistName, recordingLink))
                                break
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "" + " at BakingViewModel.")
            }
        }
    }
}