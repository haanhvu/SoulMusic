package com.haanhvu.soulmusic

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// Data model for API response
data class MusicBrainzResponse(
    val recordings: List<Recording>
)

data class Recording(
    val id: String,
    val title: String,
    val artistCredit: List<ArtistCredit> = emptyList()
)

data class ArtistCredit(
    val name: String
)

// Retrofit API interface
interface MusicBrainzApi {
    @GET("ws/2/recording/")
    suspend fun searchSongsByTag(
        @Query("query") tag: String,
        @Query("fmt") format: String = "json"
    ): MusicBrainzResponse
}

// Retrofit instance
object RetrofitClient {
    private const val BASE_URL = "https://musicbrainz.org/"

    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "SoulMusic/0.0 (haanh6594@gmail.com)")
                .build()
            chain.proceed(request)
        }
        .build()

    val api: MusicBrainzApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create()) // Converts JSON to Kotlin data classes
            .build()
            .create(MusicBrainzApi::class.java)
    }
}
