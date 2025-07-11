package com.example.thehub

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

class TheHubApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // initialize Firebase
        FirebaseApp.initializeApp(this)

        // initialize app check for debug builds
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )
    }
}
