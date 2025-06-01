package com.haanhvu.soulmusic.model

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

interface YoutubeApi {
    @POST("callYoutube")
    suspend fun callYoutube(@Body request: Map<String, String>): YouTubeSearchResponse
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

object RetrofitClientForYoutube {
    private const val BASE_URL = "https://us-central1-soulmusic-d8c81.cloudfunctions.net/"

    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val api: YoutubeApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(YoutubeApi::class.java)
    }
}