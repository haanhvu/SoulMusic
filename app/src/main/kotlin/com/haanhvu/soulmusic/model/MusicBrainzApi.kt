package com.haanhvu.soulmusic.model

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// Data model for API response
data class MusicBrainzResponse(
    val recordings: List<Recording>
)

data class Recording(
    val id: String,
    val title: String,
    @Json(name = "artist-credit")
    val artistCredit: List<NameCredit>
)

data class NameCredit(
    val name: String
)

data class RecordingUrlsResponse(
    val id: String,
    val title: String,
    val length: Long?,
    val video: Boolean,
    val relations: List<Relation>
)

data class Relation(
    val type: String,
    val url: Url,
    val direction: String
)

data class Url(
    val id: String,
    val resource: String
)

interface MusicBrainzApi {
    @GET("ws/2/recording/")
    suspend fun searchRecordingsByTag(
        @Query("query") tag: String,
        @Query("fmt") format: String = "json"
    ): MusicBrainzResponse

    @GET("ws/2/recording/{mbid}")
    suspend fun getRecordingUrls(
        @Path("mbid") recordingId: String,
        @Query("inc") include: String = "url-rels",
        @Query("fmt") format: String = "json"
    ): RecordingUrlsResponse
}

object RetrofitClientForMusicBrainz {
    private const val BASE_URL = "https://musicbrainz.org/"

    val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "SoulMusic/0.0 (haanh6594@gmail.com)")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(logging)
        .build()

    val api: MusicBrainzApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(MusicBrainzApi::class.java)
    }
}
