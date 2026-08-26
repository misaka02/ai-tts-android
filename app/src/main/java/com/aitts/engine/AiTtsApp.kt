package com.aitts.engine

import android.app.Application
import com.aitts.engine.data.ConfigDataStore

class AiTtsApp : Application() {

    companion object {
        lateinit var instance: AiTtsApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        val dataStore = ConfigDataStore.getInstance(this)
        dataStore.log("AI TTS 应用程序已启动")
    }
}
