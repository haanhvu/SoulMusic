package com.haanhvu.soulmusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.haanhvu.soulmusic.model.MusicBrainzResponse
import com.haanhvu.soulmusic.model.RetrofitClientForGemini
import com.haanhvu.soulmusic.model.RetrofitClientForMusicBrainz
import com.haanhvu.soulmusic.model.RetrofitClientForYoutube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SoulMusicViewModel : ViewModel() {
    private val _uiState: MutableStateFlow<UiState> =
        MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> =
        _uiState.asStateFlow()

    private val recordingsResult = arrayOfNulls<MusicBrainzResponse>(5)

    var recordingTitleLink = mutableMapOf<String, String>()
    private val fullRecordingTitleLink = mutableMapOf<String, String>()

    private val indexes = arrayOfNulls<Int>(5)

    fun sendPromptToGetPopularResults(
        prompt: String
    ) {
        _uiState.value = UiState.Loading

        recordingTitleLink.clear()
        fullRecordingTitleLink.clear()

        val newPrompt = prompt + ". Give me twenty results of music that can help me in this case. Only answer each result as title - artist, separate results by semicolons, nothing else."

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClientForGemini.api.callGemini(mapOf("prompt" to newPrompt))
                val geminiOutput = RetrofitClientForGemini.getOutputFromResponse(response)

                val cleanOutput = geminiOutput.replace("\"", "")
                val titleArtistList = cleanOutput.split(";")
                for (titleArtist in titleArtistList) {
                    if (fullRecordingTitleLink.size >= 5) {
                        fullRecordingTitleLink[titleArtist] = "Some link"
                        continue
                    }
                    val response = RetrofitClientForYoutube.api.callYoutube(mapOf("query" to titleArtist))
                    val video = response.items.firstOrNull()
                    var recordingLink = "Not found on Youtube"
                    video?.let {
                        recordingLink = "https://www.youtube.com/watch?v=${it.id.videoId}"
                    }
                    fullRecordingTitleLink[titleArtist] = recordingLink
                }
                recordingTitleLink = fullRecordingTitleLink.entries.take(5).associateTo(mutableMapOf()) { it.toPair() }
                _uiState.value = UiState.Success(recordingTitleLink)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "")
            }
        }
    }

    fun sendPromptToGetHiddenGems(
        prompt: String
    ) {
        _uiState.value = UiState.Loading

        recordingTitleLink.clear()
        fullRecordingTitleLink.clear()

        val newPrompt = prompt + ". Give me five most accurate keywords to search for the music that can help me in this case. Only answer those keywords, separated by commas.";

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClientForGemini.api.callGemini(mapOf("prompt" to newPrompt))
                val geminiOutput = RetrofitClientForGemini.getOutputFromResponse(response)

                val tags = geminiOutput.split(",").toTypedArray()
                for (i in tags.indices) {
                    recordingsResult[i] = RetrofitClientForMusicBrainz.api.searchRecordingsByTag("tag:" + tags[i])
                    val recordingsResultItem = recordingsResult[i]
                    recordingsResultItem?.let {
                        var index = 0
                        while (recordingsResultItem.recordings.size > 0 && index < recordingsResultItem.recordings.size) {
                            val recordingUrlsResult = RetrofitClientForMusicBrainz.api.getRecordingUrls(recordingsResultItem.recordings[index].id)
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
                                }
                                query = query + " -" + artistName
                                val response = RetrofitClientForYoutube.api.callYoutube(mapOf("query" to query))
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
                                recordingTitleLink[query] = recordingLink
                                indexes[i] = index
                                break
                            }
                        }
                    }
                }
                _uiState.value = UiState.Success(recordingTitleLink)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "")
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
            addMoreHiddenGems(stateListRecordingTitleLink)
        }
    }

    fun addMorePopularResults(
        shot: Int,
        stateListRecordingTitleLink: SnapshotStateList<Pair<String, String>>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fullRecordingTitleLinkList = fullRecordingTitleLink.entries.toList()

                for ((title, _) in fullRecordingTitleLinkList.slice(shot * 5..shot * 5 + 4)) {
                    val response = RetrofitClientForYoutube.api.callYoutube(mapOf("query" to title))
                    val video = response.items.firstOrNull()
                    var recordingLink = "Not found on Youtube"
                    video?.let {
                        recordingLink = "https://www.youtube.com/watch?v=${it.id.videoId}"
                    }
                    fullRecordingTitleLink[title] = recordingLink
                }

                stateListRecordingTitleLink.addAll(
                    fullRecordingTitleLink.entries.toList().subList(shot * 5, shot * 5 + 5)
                        .map { it.key to it.value })
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "")
            }
        }
    }

    fun addMoreHiddenGems(
        stateListRecordingTitleLink: SnapshotStateList<Pair<String, String>>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                for (i in recordingsResult.indices){
                    indexes[i] = indexes[i]!! + 1
                    var r = recordingsResult[i]
                    r?.let {
                        while (r.recordings.size > 0 && indexes[i]!! < r.recordings.size) {
                            val recordingUrlsResult = RetrofitClientForMusicBrainz.api.getRecordingUrls(r.recordings[indexes[i]!!].id)
                            if (recordingUrlsResult.relations.size > 0) {
                                val recordingTitle = r.recordings[indexes[i]!!].title
                                val recordingLink = recordingUrlsResult.relations[0].url.resource
                                stateListRecordingTitleLink.add(Pair(recordingTitle, recordingLink))
                                break
                            } else {
                                val recordingTitle = r.recordings[indexes[i]!!].title
                                if (stateListRecordingTitleLink.any { it.first == recordingTitle }) {
                                    indexes[i] = indexes[i]!! + 1
                                    continue
                                }
                                var query = recordingTitle
                                var artistName = ""
                                for (artist in r.recordings[indexes[i]!!].artistCredit) {
                                    artistName = artistName + " " + artist.name
                                }
                                query = query + " -" + artistName
                                val response = RetrofitClientForYoutube.api.callYoutube(mapOf("query" to query))
                                val video = response.items.firstOrNull()
                                var recordingLink = "Not found on Youtube"
                                var youtubeVideoTitle = ""
                                video?.let {
                                    recordingLink = "https://www.youtube.com/watch?v=${it.id.videoId}"
                                    youtubeVideoTitle = it.snippet.title
                                }
                                if (video == null || !youtubeVideoTitle.lowercase().contains(recordingTitle.lowercase())) {
                                    indexes[i] = indexes[i]!! + 1
                                    continue
                                }
                                stateListRecordingTitleLink.add(Pair(query, recordingLink))
                                break
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "")
            }
        }
    }
}