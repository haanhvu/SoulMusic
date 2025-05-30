package com.haanhvu.soulmusic

import android.app.Application
import com.google.firebase.FirebaseApp

class SoulMusic : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}