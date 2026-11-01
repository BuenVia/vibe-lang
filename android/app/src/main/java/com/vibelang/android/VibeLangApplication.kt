package com.vibelang.android

import android.app.Application
import com.vibelang.android.data.AppContainer

class VibeLangApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
