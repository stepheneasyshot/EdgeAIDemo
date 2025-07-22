package com.example.llmdemo

import android.app.Application

class LlmApp : Application() {

    companion object{
        lateinit var instance: LlmApp
    }

    init {
        instance = this
    }

}

val appContext = LlmApp.instance.applicationContext