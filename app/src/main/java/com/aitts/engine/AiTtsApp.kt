package com.aitts.engine

import android.app.Application
import com.aitts.engine.data.ConfigDataStore

class AiTtsApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val dataStore = ConfigDataStore.getInstance(this)
        dataStore.log("AI TTS 应用程序已启动")
    }
}
