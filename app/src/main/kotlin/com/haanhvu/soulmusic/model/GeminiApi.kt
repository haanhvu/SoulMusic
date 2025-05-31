package com.haanhvu.soulmusic.model

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface GeminiApi {
    @POST("callGemini")
    suspend fun callGemini(@Body request: Map<String, String>): Response<ResponseBody>
}
