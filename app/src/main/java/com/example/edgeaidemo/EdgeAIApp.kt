package com.example.edgeaidemo

import android.app.Application
import android.content.Context

class LlmApp : Application() {

    companion object{
        lateinit var instance: LlmApp
    }

    init {
        instance = this
    }

}

val appContext: Context = LlmApp.instance.applicationContext