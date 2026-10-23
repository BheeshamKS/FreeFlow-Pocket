package com.bheeshamks.freeflow

import android.app.Application
import android.util.Log

/**
 * Android Application entry point for FreeFlow Pocket.
 */
class FreeFlowApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.i("FreeFlowApplication", "FreeFlow application initialized.")
    }
}
