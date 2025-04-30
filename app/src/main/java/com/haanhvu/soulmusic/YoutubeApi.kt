package com.haanhvu.soulmusic

import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeApiService {
    @GET("search")
    suspend fun searchVideos(
        @Query("part") part: String = "snippet",
        @Query("q") query: String,
        @Query("type") type: String = "video",
        @Query("maxResults") maxResults: Int = 1,
        @Query("videoCategoryId") videoCategoryId: Int = 10,
        // If open-source, this line needs to be hidden
        @Query("key") apiKey: String = "AIzaSyDRy-p3wq8ziRwpp9hiJ8-83VxP5oHNZbY"
    ): YouTubeSearchResponse
}

data class YouTubeSearchResponse(
    val items: List<YouTubeVideoItem>
)

data class YouTubeVideoItem(
    val id: VideoId,
    val snippet: Snippet
)

data class VideoId(
    val videoId: String
)

data class Snippet(
    val title: String,
    val channelTitle: String
)