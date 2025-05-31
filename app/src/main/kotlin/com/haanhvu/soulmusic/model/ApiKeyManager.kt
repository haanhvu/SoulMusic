package com.haanhvu.soulmusic.model

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.tasks.await

class ApiKeyManager {

    private val remoteConfig = FirebaseRemoteConfig.getInstance()

    init {
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600)
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
