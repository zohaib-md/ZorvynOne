package com.project.zorvynone

import android.app.Application
import android.util.Log

/**
 * Custom Application class for Expectr.
 * Initializes global SDKs on app startup.
 */
class MyApp : Application() {

    companion object {
        private const val TAG = "MyApp"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Expectr application initialized")
    }
}
