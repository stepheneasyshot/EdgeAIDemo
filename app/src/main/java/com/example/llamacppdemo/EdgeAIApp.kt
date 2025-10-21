package com.example.llamacppdemo

import android.app.Application
import android.content.Context
import com.stephen.commonhelper.utils.LogSetting

class LlmApp : Application() {

    companion object{
        lateinit var instance: LlmApp
    }

    init {
        instance = this
    }

    override fun onCreate() {
        super.onCreate()
        LogSetting.initLogSettings("EdgeAIDemo[${BuildConfig.VERSION_NAME}]", LogSetting.LOG_VERBOSE)
    }

}

val appContext: Context = LlmApp.instance.applicationContext