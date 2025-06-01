package com.haanhvu.soulmusic.model

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

interface GeminiApi {
    @POST("callGemini")
    suspend fun callGemini(@Body request: Map<String, String>): Response<ResponseBody>
}

object RetrofitClientForGemini {
    private const val BASE_URL = "https://us-central1-soulmusic-d8c81.cloudfunctions.net/"

    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val api: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApi::class.java)
    }

    fun getOutputFromResponse(
        response: Response<ResponseBody>
    ): String {
        var output = ""
        if (response.isSuccessful) {
            val body = response.body()?.string()
            val json = JSONObject(body)
            val candidates = json.getJSONArray("candidates")
            if (candidates.length() > 0) {
                val content = candidates.getJSONObject(0).getJSONObject("content")
                val parts = content.getJSONArray("parts")
                output = parts.getJSONObject(0).getString("text")
            }
        }
        return output
    }
}
