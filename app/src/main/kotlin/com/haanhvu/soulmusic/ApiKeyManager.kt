package com.haanhvu.soulmusic

import android.content.Context
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.tasks.await

class ApiKeyManager(context: Context) {

    private val remoteConfig = FirebaseRemoteConfig.getInstance()

    init {
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(0)
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)
    }

    suspend fun fetchApiKey(): String {
        return try {
            remoteConfig.fetchAndActivate().await()
            remoteConfig.getString("api_key")
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}
