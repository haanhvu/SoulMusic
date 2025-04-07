package com.haanhvu.soulmusic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MusicViewModel : ViewModel() {
    fun fetchSongsByTag(tag: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.searchSongsByTag(tag)
                response.recordings.forEach {
                    println("Title: ${it.title}, Artist: ${it.artistCredit.joinToString { it.name }}")
                }
            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
        }
    }
}
