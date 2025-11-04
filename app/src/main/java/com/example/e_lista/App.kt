package com.example.e_lista

import android.app.Application
import com.google.firebase.database.FirebaseDatabase

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        // ⚡ Enable Firebase Realtime Database offline persistence
        val database = FirebaseDatabase.getInstance()
        database.setPersistenceEnabled(true)
    }
}